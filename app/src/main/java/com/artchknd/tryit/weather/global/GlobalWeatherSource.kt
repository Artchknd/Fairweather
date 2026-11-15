package com.artchknd.tryit.weather.global

import android.content.Context
import com.artchknd.tryit.weather.DailyForecast
import com.artchknd.tryit.weather.DataSource
import com.artchknd.tryit.weather.ForecastSlot
import com.artchknd.tryit.weather.LocationStatus
import com.artchknd.tryit.weather.PlaceForecast
import com.artchknd.tryit.weather.PlaceKey
import com.artchknd.tryit.weather.WeatherUiState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.Locale
import kotlin.math.roundToInt

private const val MAX_SEARCH_RESULTS = 6

/**
 * 境外天氣的入口：把 Open-Meteo 的資料轉成 App 現有的 UI 型別。
 * 與台灣的 CWA 流程完全獨立，只靠這個類別接進 ViewModel。
 */
class GlobalWeatherSource(context: Context) {

    private val appContext = context.applicationContext
    private val client = OpenMeteoClient()
    private val geocoder = GlobalGeocoder(appContext)

    private fun locale(): Locale = appContext.resources.configuration.locales[0]

    /** 目前位置的完整天氣（今日、36 小時時段、一週）。 */
    suspend fun load(latitude: Double, longitude: Double): Result<WeatherUiState> =
        coroutineScope {
            val placeDeferred = async { geocoder.reverse(latitude, longitude, locale()) }
            val weather = client.getWeather(latitude, longitude)
            val place = placeDeferred.await()
            weather.mapCatching { toUiState(it, place, latitude, longitude) }
        }

    /**
     * 搜尋境外地名並附上今天的摘要（一次請求取得所有結果的天氣）。
     * [excludeTaiwan] 為 true 時略過台灣的結果（那些由 CWA 的搜尋負責）。
     */
    suspend fun search(query: String, excludeTaiwan: Boolean): Result<List<PlaceForecast>> {
        val found = client.searchPlaces(query, locale()).getOrElse {
            return Result.failure(it)
        }
        val places = found
            .filterNot { excludeTaiwan && it.countryCode == "TW" }
            .take(MAX_SEARCH_RESULTS)
        if (places.isEmpty()) return Result.success(emptyList())

        // 摘要失敗時仍列出地點，只是天氣欄位顯示 "--"
        val todays = client
            .getTodaySummaries(places.map { it.latitude to it.longitude })
            .getOrNull()
        return Result.success(
            places.mapIndexed { i, p ->
                placeForecast(p.name, p.label, p.latitude, p.longitude, todays?.getOrNull(i))
            }
        )
    }

    /** 固定清單中境外地區的今日摘要；失敗時回傳空 Map，呼叫端保留舊資料。 */
    suspend fun summaries(keys: List<PlaceKey>): Map<PlaceKey, PlaceForecast> {
        val located = keys.mapNotNull { key ->
            val lat = key.latitude
            val lon = key.longitude
            if (lat != null && lon != null) Triple(key, lat, lon) else null
        }
        if (located.isEmpty()) return emptyMap()

        val todays = client
            .getTodaySummaries(located.map { it.second to it.third })
            .getOrNull() ?: return emptyMap()

        val result = HashMap<PlaceKey, PlaceForecast>()
        located.forEachIndexed { i, (key, lat, lon) ->
            val today = todays.getOrNull(i) ?: return@forEachIndexed
            result[key] = placeForecast(key.county, key.district, lat, lon, today)
        }
        return result
    }

    private fun placeForecast(
        name: String,
        label: String,
        latitude: Double,
        longitude: Double,
        today: GlobalToday?
    ): PlaceForecast = PlaceForecast(
        county = name,
        district = label,
        condition = today?.let { WmoCodes.describe(it.code, locale()) } ?: "--",
        minTemp = today?.minTemp?.roundToInt(),
        maxTemp = today?.maxTemp?.roundToInt(),
        rainChance = today?.rainChance,
        latitude = latitude,
        longitude = longitude
    )

    private fun toUiState(
        weather: GlobalWeather,
        place: GlobalPlace?,
        latitude: Double,
        longitude: Double
    ): WeatherUiState {
        val loc = locale()
        val cur = weather.current
        val today = weather.days.firstOrNull()

        return WeatherUiState(
            // 反向地理編碼失敗（例如沒有 Google 服務）時，以座標當作地名
            city = place?.name ?: String.format(Locale.US, "%.2f, %.2f", latitude, longitude),
            district = place?.label.orEmpty(),
            tempNow = cur.temp.roundToInt(),
            condition = WmoCodes.describe(cur.code, loc),
            rainChance = weather.slots.firstOrNull()?.rainChance ?: today?.rainChance,
            minTemp = today?.minTemp?.roundToInt(),
            maxTemp = today?.maxTemp?.roundToInt(),
            slots = weather.slots.map { toSlot(it, loc) },
            isLoading = false,
            error = null,
            daily = weather.days.map { toDaily(it, loc) },
            locationStatus = LocationStatus.OK,
            conditionCode = null,
            wmoCode = cur.code,
            isDay = cur.isDay,
            feelsLike = cur.feelsLike.roundToInt(),
            source = DataSource.OPEN_METEO
        )
    }

    private fun toSlot(slot: GlobalSlot, loc: Locale): ForecastSlot = ForecastSlot(
        start = cwaTime(slot.start),
        end = cwaTime(slot.end),
        condition = WmoCodes.describe(slot.code, loc),
        rainChance = slot.rainChance,
        minTemp = slot.minTemp.roundToInt(),
        maxTemp = slot.maxTemp.roundToInt(),
        comfort = comfortOf(slot.feelsLikeAvg),
        conditionCode = null,
        wmoCode = slot.code
    )

    private fun toDaily(day: GlobalDay, loc: Locale): DailyForecast = DailyForecast(
        date = day.date,
        condition = WmoCodes.describe(day.code, loc),
        rainChance = day.rainChance,
        minTemp = day.minTemp.roundToInt(),
        maxTemp = day.maxTemp.roundToInt(),
        uvIndex = day.uvIndex,
        conditionCode = null,
        wmoCode = day.code
    )

    // Open-Meteo: "2026-11-15T14:00"  ->  CWA 格式: "2026-11-15 14:00:00"
    private fun cwaTime(iso: String): String = iso.replace('T', ' ') + ":00"

    /**
     * 由體感溫度估算舒適度，輸出 CWA 的七個中文分類，
     * 這樣 WeatherText.comfort 的多語系對照可以直接沿用。
     * 這是近似值，不是 CWA 的舒適度指數。
     */
    private fun comfortOf(feelsLike: Double): String = when {
        feelsLike < 11 -> "非常寒冷"
        feelsLike < 16 -> "寒冷"
        feelsLike < 20 -> "稍有寒意"
        feelsLike < 27 -> "舒適"
        feelsLike < 31 -> "稍有悶熱"
        feelsLike < 36 -> "悶熱"
        else -> "易中暑"
    }
}