package com.artchknd.tryit.weather

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artchknd.tryit.R
import com.artchknd.tryit.location.LocationResolver
import com.artchknd.tryit.location.LocationResult
import com.artchknd.tryit.weather.global.GlobalWeatherSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.UnknownHostException

private const val MAX_PINS = 10
private const val PINNED_STALE_MS = 5 * 60 * 1000L

// 台灣鄉鎮名稱只有中文；沒有中文字的查詢不需要呼叫 CWA
private fun containsCjk(text: String): Boolean = text.any { it.code in 0x4E00..0x9FFF }

private fun DailyForecast.toPlaceForecast(key: PlaceKey): PlaceForecast = PlaceForecast(
    county = key.county,
    district = key.district,
    condition = condition,
    minTemp = minTemp,
    maxTemp = maxTemp,
    rainChance = rainChance
)

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    // 台灣（CWA）
    private val repo = WeatherRepository()
    private val weeklyRepo = WeeklyRepository()
    private val searchRepo = PlaceSearchRepository(application, weeklyRepo)

    // 境外（Open-Meteo），與上面完全獨立
    private val globalSource = GlobalWeatherSource(application)

    private val resolver = LocationResolver(application)
    private val pinnedStore = PinnedStore(application)

    private val _state = MutableStateFlow(WeatherUiState())
    val state: StateFlow<WeatherUiState> = _state.asStateFlow()

    private val _search = MutableStateFlow(SearchUiState())
    val search: StateFlow<SearchUiState> = _search.asStateFlow()

    // 啟動時先用本機存的識別建立占位項目，預報載入後再補上
    private val _pinned = MutableStateFlow(
        pinnedStore.load().map { PlaceForecast.placeholder(it) }
    )
    val pinned: StateFlow<List<PlaceForecast>> = _pinned.asStateFlow()

    // 一次性訊息（例如固定數量已達上限），由畫面層以 Toast 顯示
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var pinnedJob: Job? = null
    private var pinnedRefreshedAt = 0L

    init {
        refresh()
        refreshPinned()
    }

    fun refresh() {
        // 新的重新整理會取消進行中的，避免舊結果覆蓋新結果
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val location = resolver.resolve()) {
                is LocationResult.Success -> loadWeather(location.county, location.district)
                is LocationResult.OutsideTaiwan ->
                    loadGlobalWeather(location.latitude, location.longitude)
                LocationResult.PermissionDenied -> _state.value = WeatherUiState(
                    isLoading = false,
                    locationStatus = LocationStatus.PERMISSION_REQUIRED
                )
                LocationResult.LocationUnavailable ->
                    showError(getApplication<Application>().getString(R.string.error_location_unavailable))
                LocationResult.LookupFailed ->
                    showError(getApplication<Application>().getString(R.string.error_location_lookup_failed))
            }
        }
    }

    /** 從系統設定或權限對話框回來時呼叫：只有位置被擋住的狀態才重新偵測。 */
    fun refreshIfLocationBlocked() {
        if (_state.value.locationStatus != LocationStatus.OK) refresh()
    }

    /**
     * 搜尋指定地區的天氣，對應畫面的 onSearch。
     * 台灣（中文查詢）與境外同時搜尋，結果合併：台灣在前、境外在後。
     */
    fun searchLocation(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _search.value = SearchUiState(isSearching = true)
            val chinese = containsCjk(text)
            val (taiwan, global) = coroutineScope {
                val tw = async {
                    if (chinese) searchRepo.search(text)
                    else Result.success(emptyList<PlaceForecast>())
                }
                // 中文查詢的台灣結果由 CWA 負責，境外搜尋略過台灣，避免同一地點出現兩個資料源
                val gl = async { globalSource.search(text, excludeTaiwan = chinese) }
                tw.await() to gl.await()
            }
            val results = taiwan.getOrDefault(emptyList()) + global.getOrDefault(emptyList())
            val failure = taiwan.exceptionOrNull() ?: global.exceptionOrNull()
            _search.value =
                if (results.isEmpty() && failure != null) {
                    SearchUiState(error = errorMessage(failure, R.string.error_search_failed))
                } else {
                    SearchUiState(results = results)
                }
        }
    }

    /**
     * 固定或取消固定，對應畫面的 onTogglePin。
     * 以「縣市|鄉鎮」（境外為「地名|標示」）判斷是否已固定，畫面端的呼叫方式不變；
     * 境外項目的座標從搜尋結果帶入。
     */
    fun togglePin(county: String, district: String) {
        val current = _pinned.value
        val exists = current.any { it.county == county && it.district == district }
        if (exists) {
            _pinned.value = current.filterNot { it.county == county && it.district == district }
        } else {
            if (current.size >= MAX_PINS) {
                _messages.tryEmit(
                    getApplication<Application>().getString(R.string.pinned_max_limit_reached, MAX_PINS)
                )
                return
            }
            // 從搜尋結果固定時直接帶入已有的預報（含座標），不必再請求一次
            val item = _search.value.results
                .firstOrNull { it.county == county && it.district == district }
            _pinned.value = current + (item ?: PlaceForecast.placeholder(PlaceKey(county, district)))
            if (item == null) refreshPinned()
        }
        pinnedStore.save(_pinned.value.map { it.key })
    }

    /** 重新抓所有固定地區的預報；單一地區失敗時保留舊資料，不影響其他地區。 */
    fun refreshPinned() {
        val keys = _pinned.value.map { it.key }
        if (keys.isEmpty()) return
        pinnedRefreshedAt = System.currentTimeMillis()
        pinnedJob?.cancel()
        pinnedJob = viewModelScope.launch {
            val updates: Map<PlaceKey, PlaceForecast> = coroutineScope {
                // 境外的固定地區一次請求全部取回
                val globalDeferred = async { globalSource.summaries(keys.filter { it.isGlobal }) }
                val taiwanDeferred = keys.filterNot { it.isGlobal }.map { key ->
                    async {
                        weeklyRepo.getWeekly(key.county, key.district)
                            .getOrNull()?.firstOrNull()
                            ?.let { today -> key to today.toPlaceForecast(key) }
                    }
                }
                taiwanDeferred.awaitAll().filterNotNull().toMap() + globalDeferred.await()
            }
            _pinned.update { list ->
                list.map { item ->
                    val today = updates[item.key]
                    if (today == null) item else item.copy(
                        condition = today.condition,
                        minTemp = today.minTemp,
                        maxTemp = today.maxTemp,
                        rainChance = today.rainChance
                    )
                }
            }
        }
    }

    /** 回到前景時呼叫：距離上次更新超過 5 分鐘才重抓。 */
    fun refreshPinnedIfStale() {
        if (System.currentTimeMillis() - pinnedRefreshedAt > PINNED_STALE_MS) refreshPinned()
    }

    private suspend fun loadWeather(county: String, district: String) = coroutineScope {
        // 今日預報與週預報互不相依，同時發出請求，總等待時間由兩者相加變成取較長的一個
        val weeklyDeferred = async { weeklyRepo.getWeekly(county, district) }
        val today = repo.getForecast(county)

        today
            .onSuccess { ui ->
                _state.update { old ->
                    // 同一個地點才保留上一次的週預報；換了地點就清掉
                    val sameSpot = old.city == ui.city && old.district == district
                    ui.copy(
                        district = district,
                        daily = if (sameSpot) old.daily else emptyList(),
                        source = DataSource.TAIWAN_CWA
                    )
                }
                // 週預報失敗不影響主畫面，維持原本的清單
                weeklyDeferred.await().onSuccess { weekly ->
                    _state.update { it.copy(daily = weekly) }
                }
            }
            .onFailure { e ->
                weeklyDeferred.cancel()
                showError(errorMessage(e, R.string.error_weather_fetch_failed))
            }
    }

    private suspend fun loadGlobalWeather(latitude: Double, longitude: Double) {
        globalSource.load(latitude, longitude)
            .onSuccess { ui -> _state.value = ui }
            .onFailure { e -> showError(errorMessage(e, R.string.error_weather_fetch_failed)) }
    }

    private fun errorMessage(e: Throwable, @StringRes fallback: Int): String {
        val app = getApplication<Application>()
        return when {
            e is UnknownHostException -> app.getString(R.string.error_network_unavailable)
            e.message?.startsWith("HTTP 401") == true -> app.getString(R.string.error_invalid_api_key)
            else -> app.getString(fallback)
        }
    }

    private fun showError(message: String) {
        _state.update {
            it.copy(isLoading = false, error = message, locationStatus = LocationStatus.OK)
        }
    }
}