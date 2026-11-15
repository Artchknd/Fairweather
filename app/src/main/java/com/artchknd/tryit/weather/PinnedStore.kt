package com.artchknd.tryit.weather

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

private const val PREFS_NAME = "fairweather_pinned"
private const val KEY_PINS = "pins"

class PinnedStore(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun load(): List<PlaceKey> = runCatching {
        val array = JSONArray(prefs.getString(KEY_PINS, "[]"))
        (0 until array.length()).mapNotNull { i ->
            val obj = array.getJSONObject(i)
            val county = obj.optString("c")
            val district = obj.optString("d")
            if (county.isBlank() || district.isBlank()) null else PlaceKey(county, district)
        }
    }.getOrDefault(emptyList())

    fun save(keys: List<PlaceKey>) {
        val array = JSONArray()
        keys.forEach { array.put(JSONObject().put("c", it.county).put("d", it.district)) }
        prefs.edit().putString(KEY_PINS, array.toString()).apply()
    }
}
