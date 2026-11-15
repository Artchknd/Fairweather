package com.artchknd.tryit.weather

import android.app.Application
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.artchknd.tryit.R
import com.artchknd.tryit.location.LocationResolver
import com.artchknd.tryit.location.LocationResult
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

class WeatherViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = WeatherRepository()
    private val weeklyRepo = WeeklyRepository()
    private val resolver = LocationResolver(application)
    private val searchRepo = PlaceSearchRepository(application, weeklyRepo)
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
                LocationResult.PermissionDenied -> _state.value = WeatherUiState(
                    isLoading = false,
                    locationStatus = LocationStatus.PERMISSION_REQUIRED
                )
                LocationResult.OutsideTaiwan -> _state.value = WeatherUiState(
                    isLoading = false,
                    locationStatus = LocationStatus.UNSUPPORTED_REGION
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

    /** 搜尋指定地區的天氣，對應畫面的 onSearch。 */
    fun searchLocation(query: String) {
        val text = query.trim()
        if (text.isEmpty()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _search.value = SearchUiState(isSearching = true)
            searchRepo.search(text)
                .onSuccess { list -> _search.value = SearchUiState(results = list) }
                .onFailure { e ->
                    val msg = when {
                        e is UnknownHostException -> getApplication<Application>().getString(R.string.error_network_unavailable)
                        e.message?.startsWith("HTTP 401") == true -> getApplication<Application>().getString(R.string.error_invalid_api_key)
                        else -> getApplication<Application>().getString(R.string.error_search_failed)
                    }
                    _search.value = SearchUiState(error = msg)
                }
        }
    }

    /** 固定或取消固定，對應畫面的 onTogglePin。以「縣市|鄉鎮」判斷是否已固定。 */
    fun togglePin(county: String, district: String) {
        val key = PlaceKey(county, district)
        val current = _pinned.value
        if (current.any { it.key == key }) {
            _pinned.value = current.filterNot { it.key == key }
        } else {
            if (current.size >= MAX_PINS) {
                _messages.tryEmit(
                    getApplication<Application>().getString(R.string.pinned_max_limit_reached, MAX_PINS)
                )
                return
            }
            // 從搜尋結果固定時直接帶入已有的預報，不必再請求一次
            val item = _search.value.results.firstOrNull { it.key == key }
            _pinned.value = current + (item ?: PlaceForecast.placeholder(key))
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
            val fetched = coroutineScope {
                keys.map { key ->
                    async {
                        key to weeklyRepo.getWeekly(key.county, key.district)
                            .getOrNull()?.firstOrNull()
                    }
                }.awaitAll()
            }
            _pinned.update { list ->
                list.map { item ->
                    val today = fetched.firstOrNull { it.first == item.key }?.second
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
                    ui.copy(district = district, daily = if (sameSpot) old.daily else emptyList())
                }
                // 週預報失敗不影響主畫面，維持原本的清單
                weeklyDeferred.await().onSuccess { weekly ->
                    _state.update { it.copy(daily = weekly) }
                }
            }
            .onFailure { e ->
                weeklyDeferred.cancel()
                val msg = when {
                    e is UnknownHostException -> getApplication<Application>().getString(R.string.error_network_unavailable)
                    e.message?.startsWith("HTTP 401") == true -> getApplication<Application>().getString(R.string.error_invalid_api_key)
                    else -> getApplication<Application>().getString(R.string.error_weather_fetch_failed)
                }
                showError(msg)
            }
    }

    private fun showError(message: String) {
        _state.update {
            it.copy(isLoading = false, error = message, locationStatus = LocationStatus.OK)
        }
    }
}