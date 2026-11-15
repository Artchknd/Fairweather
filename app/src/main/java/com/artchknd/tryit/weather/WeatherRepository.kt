package com.artchknd.tryit.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder

class WeatherRepository {

    suspend fun getForecast(county: String): Result<WeatherUiState> =
        withContext(Dispatchers.IO) {
            runCatching { parse(fetch(county)) }
        }

    private fun fetch(county: String): String {
        val q = URLEncoder.encode(county, "UTF-8")
        val url = CwaEndpoint.build("F-C0032-001", "locationName=$q")
        val conn = url.openConnection() as HttpURLConnection
        try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            // 先檢查狀態碼再讀內容，避免例外訊息帶出含 key 的網址
            if (conn.responseCode != 200) throw IOException("HTTP ${conn.responseCode}")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parse(body: String): WeatherUiState {
        val loc = JSONObject(body)
            .optJSONObject("records")
            ?.optJSONArray("location")
            ?.optJSONObject(0)
            ?: throw IOException("no data")

        val elements = loc.getJSONArray("weatherElement").objects()
        fun slots(name: String): List<JSONObject> =
            elements.firstOrNull { it.getString("elementName") == name }
                ?.getJSONArray("time")?.objects().orEmpty()

        val wx = slots("Wx")
        val pop = slots("PoP")
        val minT = slots("MinT")
        val maxT = slots("MaxT")
        val ci = slots("CI")

        val list = wx.mapIndexed { i, w ->
            ForecastSlot(
                start = w.getString("startTime"),
                end = w.getString("endTime"),
                condition = w.value(),
                conditionCode = w.code(),
                rainChance = pop.getOrNull(i)?.value()?.toIntOrNull(),
                minTemp = minT.getOrNull(i)?.value()?.toIntOrNull(),
                maxTemp = maxT.getOrNull(i)?.value()?.toIntOrNull(),
                comfort = ci.getOrNull(i)?.value().orEmpty()
            )
        }
        val first = list.firstOrNull() ?: throw IOException("empty forecast")

        return WeatherUiState(
            city = loc.getString("locationName"),
            condition = first.condition,
            conditionCode = first.conditionCode,
            rainChance = first.rainChance,
            minTemp = first.minTemp,
            maxTemp = first.maxTemp,
            slots = list,
            isLoading = false
        )
    }
}

private fun JSONArray.objects(): List<JSONObject> = List(length()) { getJSONObject(it) }

private fun JSONObject.value(): String =
    getJSONObject("parameter").getString("parameterName")

private fun JSONObject.code(): Int? =
    getJSONObject("parameter").optString("parameterValue").toIntOrNull()