package com.artchknd.tryit.weather

import com.artchknd.tryit.location.TaiwanRegions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder

data class DailyForecast(
    val date: String,        // yyyy-MM-dd
    val condition: String,
    val rainChance: Int?,    // 台灣 3 天以後為 null
    val minTemp: Int,
    val maxTemp: Int,
    val uvIndex: Int?,       // 當天只剩夜間時段時為 null
    val conditionCode: Int? = null,  // CWA WeatherCode，用於多語系顯示（台灣資料）
    val wmoCode: Int? = null         // WMO 天氣代碼（境外資料）
)

private typealias Series = List<Pair<String, String>>

// 搜尋整個縣市時只要這四項，大幅縮小回應大小（不含紫外線，uvIndex 會是 null）
private const val COUNTY_ELEMENTS = "最高溫度,最低溫度,12小時降雨機率,天氣現象"

private fun JSONObject.elementSeries(name: String, key: String): Series {
    val elements = getJSONArray("WeatherElement")
    for (i in 0 until elements.length()) {
        val el = elements.getJSONObject(i)
        if (el.optString("ElementName") != name) continue
        val times = el.getJSONArray("Time")
        return (0 until times.length()).map { t ->
            val item = times.getJSONObject(t)
            val value = item.getJSONArray("ElementValue").getJSONObject(0).optString(key, "-")
            item.optString("StartTime") to value
        }
    }
    return emptyList()
}

private fun Series.ofDay(day: String): Series = filter { it.first.startsWith(day) }

private fun Series.ints(): List<Int> = mapNotNull { it.second.toIntOrNull() }

class WeeklyRepository {

    /** 單一鄉鎮的一週預報。 */
    suspend fun getWeekly(county: String, district: String): Result<List<DailyForecast>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = request(
                    county,
                    "&LocationName=${URLEncoder.encode(district, "UTF-8")}"
                )
                parseAll(text).first().second
            }
        }

    /** 整個縣市所有鄉鎮的一週預報（鄉鎮名稱 to 預報），一次請求。 */
    suspend fun getCountyWeekly(county: String): Result<Map<String, List<DailyForecast>>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val text = request(
                    county,
                    "&ElementName=${URLEncoder.encode(COUNTY_ELEMENTS, "UTF-8")}"
                )
                parseAll(text).toMap()
            }
        }

    private fun request(county: String, extraQuery: String): String {
        val dataset = TaiwanRegions.weeklyDatasetId(county) ?: error("unsupported county")
        val url = CwaEndpoint.build(dataset, "format=JSON$extraQuery")
        val conn = url.openConnection() as HttpURLConnection
        conn.connectTimeout = 10_000
        conn.readTimeout = 15_000
        conn.setRequestProperty("User-Agent", "Fairweather/1.0")
        return try {
            // 先檢查狀態碼再讀內容，避免例外訊息帶出含 key 的網址
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseAll(text: String): List<Pair<String, List<DailyForecast>>> {
        val locations = JSONObject(text).getJSONObject("records")
            .getJSONArray("Locations").getJSONObject(0)
            .getJSONArray("Location")
        return (0 until locations.length()).map { i ->
            val location = locations.getJSONObject(i)
            location.optString("LocationName") to parseLocation(location)
        }
    }

    private fun parseLocation(location: JSONObject): List<DailyForecast> {
        val maxT = location.elementSeries("最高溫度", "MaxTemperature")
        val minT = location.elementSeries("最低溫度", "MinTemperature")
        val pop = location.elementSeries("12小時降雨機率", "ProbabilityOfPrecipitation")
        val wx = location.elementSeries("天氣現象", "Weather")
        val wxCode = location.elementSeries("天氣現象", "WeatherCode")
        val uv = location.elementSeries("紫外線指數", "UVIndex")

        return maxT.map { it.first.take(10) }.distinct().mapNotNull { day ->
            val highs = maxT.ofDay(day).ints()
            val lows = minT.ofDay(day).ints()
            if (highs.isEmpty() || lows.isEmpty()) return@mapNotNull null

            val dayWx = wx.ofDay(day)
            val chosen = dayWx.firstOrNull { it.first.substring(11, 13) == "06" }
                ?: dayWx.firstOrNull()
            val condition = chosen?.second ?: "--"
            val conditionCode = chosen?.let { c ->
                wxCode.firstOrNull { it.first == c.first }?.second?.toIntOrNull()
            }

            DailyForecast(
                date = day,
                condition = condition,
                rainChance = pop.ofDay(day).ints().maxOrNull(),
                minTemp = lows.min(),
                maxTemp = highs.max(),
                uvIndex = uv.ofDay(day).ints().maxOrNull(),
                conditionCode = conditionCode
            )
        }
    }
}