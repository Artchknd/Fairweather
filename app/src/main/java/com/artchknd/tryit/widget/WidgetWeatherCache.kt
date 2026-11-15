package com.artchknd.tryit.widget

import android.content.Context
import com.artchknd.tryit.weather.ForecastSlot
import com.artchknd.tryit.weather.LocationStatus
import com.artchknd.tryit.weather.WeatherUiState
import org.json.JSONArray
import org.json.JSONObject

data class WidgetSnapshot(
    val city: String,
    val district: String,
    val tempNow: Int?,
    val condition: String,
    val conditionCode: Int? = null,
    val wmoCode: Int? = null,
    val isDay: Boolean = true,
    val rainChance: Int?,
    val minTemp: Int?,
    val maxTemp: Int?,
    val slots: List<ForecastSlot>,
    val locationStatus: LocationStatus,
    val updatedAtMillis: Long
)

object WidgetWeatherCache {
    private const val PREFS_NAME = "fairweather_widget_cache"
    private const val KEY_SNAPSHOT = "snapshot_json"

    fun saveFromUiState(context: Context, state: WeatherUiState) {
        if (state.isLoading && state.slots.isEmpty() && state.locationStatus == LocationStatus.OK) {
            return
        }
        val previous = load(context)
        val shouldKeepPreviousSlots = state.error != null && state.slots.isEmpty() && previous != null
        val snapshot = if (shouldKeepPreviousSlots) {
            previous.copy(updatedAtMillis = System.currentTimeMillis())
        } else {
            WidgetSnapshot(
                city = state.city,
                district = state.district,
                tempNow = state.tempNow,
                condition = state.condition,
                conditionCode = state.conditionCode,
                wmoCode = state.wmoCode,
                isDay = state.isDay,
                rainChance = state.rainChance,
                minTemp = state.minTemp,
                maxTemp = state.maxTemp,
                slots = state.slots.take(3),
                locationStatus = state.locationStatus,
                updatedAtMillis = System.currentTimeMillis()
            )
        }
        save(context, snapshot)
    }

    fun save(context: Context, snapshot: WidgetSnapshot) {
        val json = JSONObject().apply {
            put("city", snapshot.city)
            put("district", snapshot.district)
            putNullableInt("tempNow", snapshot.tempNow)
            put("condition", snapshot.condition)
            putNullableInt("conditionCode", snapshot.conditionCode)
            putNullableInt("wmoCode", snapshot.wmoCode)
            put("isDay", snapshot.isDay)
            putNullableInt("rainChance", snapshot.rainChance)
            putNullableInt("minTemp", snapshot.minTemp)
            putNullableInt("maxTemp", snapshot.maxTemp)
            put("locationStatus", snapshot.locationStatus.name)
            put("updatedAtMillis", snapshot.updatedAtMillis)
            val arr = JSONArray()
            snapshot.slots.take(3).forEach { slot ->
                arr.put(
                    JSONObject().apply {
                        put("start", slot.start)
                        put("end", slot.end)
                        put("condition", slot.condition)
                        putNullableInt("rainChance", slot.rainChance)
                        putNullableInt("minTemp", slot.minTemp)
                        putNullableInt("maxTemp", slot.maxTemp)
                        put("comfort", slot.comfort)
                        putNullableInt("conditionCode", slot.conditionCode)
                        putNullableInt("wmoCode", slot.wmoCode)
                    }
                )
            }
            put("slots", arr)
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SNAPSHOT, json.toString())
            .apply()
    }

    fun load(context: Context): WidgetSnapshot? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SNAPSHOT, null) ?: return null
        return try {
            val json = JSONObject(raw)
            val arr = json.optJSONArray("slots") ?: JSONArray()
            val slots = buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        ForecastSlot(
                            start = obj.optString("start"),
                            end = obj.optString("end"),
                            condition = obj.optString("condition"),
                            rainChance = obj.optNullableInt("rainChance"),
                            minTemp = obj.optNullableInt("minTemp"),
                            maxTemp = obj.optNullableInt("maxTemp"),
                            comfort = obj.optString("comfort"),
                            conditionCode = obj.optNullableInt("conditionCode"),
                            wmoCode = obj.optNullableInt("wmoCode")
                        )
                    )
                }
            }
            val statusName = json.optString("locationStatus", LocationStatus.OK.name)
            val status = LocationStatus.entries.firstOrNull { it.name == statusName }
                ?: LocationStatus.OK

            WidgetSnapshot(
                city = json.optString("city"),
                district = json.optString("district"),
                tempNow = json.optNullableInt("tempNow"),
                condition = json.optString("condition"),
                conditionCode = json.optNullableInt("conditionCode"),
                wmoCode = json.optNullableInt("wmoCode"),
                isDay = json.optBoolean("isDay", true),
                rainChance = json.optNullableInt("rainChance"),
                minTemp = json.optNullableInt("minTemp"),
                maxTemp = json.optNullableInt("maxTemp"),
                slots = slots,
                locationStatus = status,
                updatedAtMillis = json.optLong("updatedAtMillis", 0L)
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun JSONObject.putNullableInt(key: String, value: Int?) {
        if (value == null) put(key, JSONObject.NULL) else put(key, value)
    }

    private fun JSONObject.optNullableInt(key: String): Int? {
        return if (isNull(key) || !has(key)) null else optInt(key)
    }
}