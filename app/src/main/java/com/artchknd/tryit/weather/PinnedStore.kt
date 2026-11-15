package com.artchknd.tryit.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "fairweather_pinned"
private const val KEY_PINS = "pins"

class PinnedStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // 舊版只存 c / d（縣市、鄉鎮）；境外項目多存 la / lo（座標），兩種格式都能讀
    fun load(): List<PlaceKey> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PINS, "[]"))
        (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val county = obj.optString("c")
            val district = obj.optString("d")
            val lat = obj.optDouble("la", Double.NaN)
            val lon = obj.optDouble("lo", Double.NaN)
            when {
                county.isBlank() -> null
                !lat.isNaN() && !lon.isNaN() -> PlaceKey(county, district, lat, lon)
                district.isBlank() -> null
                else -> PlaceKey(county, district)
            }
        }
    }.getOrDefault(emptyList())

    fun save(keys: List<PlaceKey>) {
        val array = JSONArray()
        keys.forEach { key ->
            val obj = JSONObject().put("c", key.county).put("d", key.district)
            val lat = key.latitude
            val lon = key.longitude
            if (lat != null && lon != null) obj.put("la", lat).put("lo", lon)
            array.put(obj)
        }
        prefs.edit().putString(KEY_PINS, array.toString()).apply()
    }
}