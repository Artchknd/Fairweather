package com.artchknd.tryit.weather.global

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.roundToInt

private const val FORECAST_URL = "https://api.open-meteo.com/v1/forecast"
private const val GEOCODING_URL = "https://geocoding-api.open-meteo.com/v1/search"
private const val USER_AGENT = "Fairweather/1.0 (https://github.com/Artchknd/Fairweather)"
private const val TIMEOUT_MS = 10_000
private const val SLOT_COUNT = 3
private const val SLOT_HOURS = 12
private const val BOUNDARY_HOUR = 6

class OpenMeteoClient {

    suspend fun getWeather(latitude: Double, longitude: Double): Result<GlobalWeather> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = FORECAST_URL +
                        "?latitude=${coord(latitude)}&longitude=${coord(longitude)}" +
                        "&current=temperature_2m,apparent_temperature,relative_humidity_2m," +
                        "is_day,weather_code,wind_speed_10m" +
                        "&hourly=temperature_2m,apparent_temperature," +
                        "precipitation_probability,weather_code" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                        "precipitation_probability_max,uv_index_max" +
                        "&timezone=auto&forecast_days=7"
                parseWeather(httpGet(url))
            }
        }

    /**
     * 多個地點「今天」的摘要，一次請求。
     * 回傳的清單與 [points] 同順序；單一地點解析失敗時該位置為 null。
     */
    suspend fun getTodaySummaries(
        points: List<Pair<Double, Double>>
    ): Result<List<GlobalToday?>> = withContext(Dispatchers.IO) {
        runCatching {
            if (points.isEmpty()) {
                emptyList<GlobalToday?>()
            } else {
                val lats = points.joinToString(",") { coord(it.first) }
                val lons = points.joinToString(",") { coord(it.second) }
                val url = FORECAST_URL +
                        "?latitude=$lats&longitude=$lons" +
                        "&daily=weather_code,temperature_2m_max,temperature_2m_min," +
                        "precipitation_probability_max" +
                        "&timezone=auto&forecast_days=1"
                val body = httpGet(url)
                // 單一地點回傳物件，多個地點回傳陣列
                val roots = if (body.trimStart().startsWith("[")) {
                    val array = JSONArray(body)
                    List(array.length()) { array.getJSONObject(it) }
                } else {
                    listOf(JSONObject(body))
                }
                roots.map { parseToday(it) }
            }
        }
    }

    suspend fun searchPlaces(
        query: String,
        locale: Locale = Locale.getDefault(),
        count: Int = 10
    ): Result<List<GlobalPlace>> = withContext(Dispatchers.IO) {
        runCatching {
            val q = query.trim()
            if (q.isEmpty()) {
                emptyList()
            } else {
                val lang = if (locale.language == "zh") "zh" else "en"
                val url = GEOCODING_URL +
                        "?name=${URLEncoder.encode(q, "UTF-8")}" +
                        "&count=$count&language=$lang&format=json"
                val results = JSONObject(httpGet(url)).optJSONArray("results")
                val list = ArrayList<GlobalPlace>()
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val r = results.getJSONObject(i)
                        list += GlobalPlace(
                            name = r.getString("name"),
                            admin = r.optString("admin1").ifBlank { null },
                            country = r.optString("country").ifBlank { null },
                            countryCode = r.optString("country_code").ifBlank { null },
                            latitude = r.getDouble("latitude"),
                            longitude = r.getDouble("longitude")
                        )
                    }
                }
                list
            }
        }
    }

    // 座標四捨五入到小數 2 位（約 1 公里），不把精確位置送出去
    private fun coord(value: Double): String = String.format(Locale.US, "%.2f", value)

    private fun httpGet(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("User-Agent", USER_AGENT)
            val code = conn.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseToday(root: JSONObject): GlobalToday? {
        val d = root.optJSONObject("daily") ?: return null
        val max = d.getJSONArray("temperature_2m_max").doubleOrNull(0) ?: return null
        val min = d.getJSONArray("temperature_2m_min").doubleOrNull(0) ?: return null
        val code = d.getJSONArray("weather_code").doubleOrNull(0)?.roundToInt() ?: 0
        val pop = d.getJSONArray("precipitation_probability_max").doubleOrNull(0)?.roundToInt()
        return GlobalToday(code = code, minTemp = min, maxTemp = max, rainChance = pop)
    }

    private fun parseWeather(body: String): GlobalWeather {
        val root = JSONObject(body)

        val cur = root.getJSONObject("current")
        val curTemp = cur.getDouble("temperature_2m")
        val current = GlobalCurrent(
            time = cur.getString("time"),
            temp = curTemp,
            feelsLike = cur.doubleOrNull("apparent_temperature") ?: curTemp,
            code = cur.getInt("weather_code"),
            isDay = cur.optInt("is_day", 1) == 1,
            humidity = cur.doubleOrNull("relative_humidity_2m")?.roundToInt(),
            windKmh = cur.doubleOrNull("wind_speed_10m")
        )

        val h = root.getJSONObject("hourly")
        val times = h.getJSONArray("time")
        val temps = h.getJSONArray("temperature_2m")
        val feels = h.getJSONArray("apparent_temperature")
        val pops = h.getJSONArray("precipitation_probability")
        val codes = h.getJSONArray("weather_code")

        // 逐小時時間是該地點當地時間的 ISO 字串，取「yyyy-MM-ddTHH」前綴做字典序比較即可
        val nowHour = current.time.take(13)
        var startIdx = 0
        for (i in 0 until times.length()) {
            if (times.getString(i).take(13) >= nowHour) {
                startIdx = i
                break
            }
        }

        // 切成三段 12 小時：第一段從現在到下一個 06:00 或 18:00，
        // 之後的邊界都落在 06 / 18 點，和 CWA 的 36 小時預報時段一致
        val firstHour = times.getString(startIdx).substring(11, 13).toInt()
        val untilBoundary = ((BOUNDARY_HOUR - firstHour) % SLOT_HOURS + SLOT_HOURS) % SLOT_HOURS
        val firstLength = if (untilBoundary == 0) SLOT_HOURS else untilBoundary
        val bounds = List(SLOT_COUNT + 1) { i ->
            if (i == 0) startIdx else startIdx + firstLength + (i - 1) * SLOT_HOURS
        }

        val slots = ArrayList<GlobalSlot>()
        for (b in 0 until SLOT_COUNT) {
            val from = bounds[b]
            val to = minOf(bounds[b + 1], times.length())
            if (from >= to) break

            var minT = Double.MAX_VALUE
            var maxT = -Double.MAX_VALUE
            var feelSum = 0.0
            var feelCount = 0
            var pop: Int? = null
            var worst = 0
            for (i in from until to) {
                val t = temps.doubleOrNull(i)
                if (t != null) {
                    if (t < minT) minT = t
                    if (t > maxT) maxT = t
                }
                val f = feels.doubleOrNull(i)
                if (f != null) {
                    feelSum += f
                    feelCount++
                }
                val p = pops.doubleOrNull(i)?.roundToInt()
                if (p != null && (pop == null || p > pop)) pop = p
                if (!codes.isNull(i)) worst = maxOf(worst, codes.getInt(i))
            }
            if (minT == Double.MAX_VALUE) continue

            slots += GlobalSlot(
                start = times.getString(from),
                end = times.getString(minOf(to, times.length() - 1)),
                code = worst,
                rainChance = pop,
                minTemp = minT,
                maxTemp = maxT,
                feelsLikeAvg = if (feelCount > 0) feelSum / feelCount else (minT + maxT) / 2
            )
        }

        // 「今天」這一列只看現在到今天結束的小時：已經過去的小時（例如清晨的毛毛雨）
        // 不應該讓今天的天氣和降雨機率與上方的目前天氣、時段互相矛盾
        val todayDate = current.time.take(10)
        var todayCode = -1
        var todayPop: Int? = null
        for (i in startIdx until times.length()) {
            if (!times.getString(i).startsWith(todayDate)) break
            val p = pops.doubleOrNull(i)?.roundToInt()
            if (p != null && (todayPop == null || p > todayPop)) todayPop = p
            if (!codes.isNull(i)) todayCode = maxOf(todayCode, codes.getInt(i))
        }

        val d = root.getJSONObject("daily")
        val dTimes = d.getJSONArray("time")
        val dCodes = d.getJSONArray("weather_code")
        val dMax = d.getJSONArray("temperature_2m_max")
        val dMin = d.getJSONArray("temperature_2m_min")
        val dPop = d.getJSONArray("precipitation_probability_max")
        val dUv = d.getJSONArray("uv_index_max")

        val days = ArrayList<GlobalDay>()
        for (i in 0 until dTimes.length()) {
            val max = dMax.doubleOrNull(i) ?: continue
            val min = dMin.doubleOrNull(i) ?: continue
            val date = dTimes.getString(i)
            val isToday = date == todayDate && todayCode >= 0
            val dailyPop = dPop.doubleOrNull(i)?.roundToInt()
            days += GlobalDay(
                date = date,
                code = when {
                    isToday -> todayCode
                    dCodes.isNull(i) -> 0
                    else -> dCodes.getInt(i)
                },
                rainChance = if (isToday) (todayPop ?: dailyPop) else dailyPop,
                minTemp = min,
                maxTemp = max,
                uvIndex = dUv.doubleOrNull(i)?.roundToInt()
            )
        }

        return GlobalWeather(current = current, slots = slots, days = days)
    }

    private fun JSONObject.doubleOrNull(key: String): Double? =
        if (has(key) && !isNull(key)) getDouble(key) else null

    private fun JSONArray.doubleOrNull(index: Int): Double? =
        if (index < length() && !isNull(index)) getDouble(index) else null
}