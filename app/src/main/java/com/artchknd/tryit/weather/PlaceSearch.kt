package com.artchknd.tryit.weather

import android.content.Context
import com.artchknd.tryit.location.TaiwanRegions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val PREFS_NAME = "fairweather_district_index"
private const val KEY_INDEX = "index"
private const val CACHE_TTL_MS = 30 * 60 * 1000L
private const val MAX_PARALLEL = 6
private const val MAX_RESULTS = 40

// 單獨輸入「市 / 區 / 鄉 / 鎮 / 縣」時，視為鄉鎮名稱的一部分（例如「花蓮市」「桃園區」）
private val GENERIC_SUFFIXES = setOf("市", "區", "鄉", "鎮", "縣")

/**
 * 搜尋鄉鎮的天氣。
 * 每個縣市一次請求就能取得所有鄉鎮的一週預報，所以搜尋與摘要同一次完成。
 * - 縣市資料在記憶體快取 30 分鐘。
 * - 「縣市 -> 鄉鎮名稱」索引存在本機，之後只輸入鄉鎮名時，只會抓有可能符合的縣市。
 */
class PlaceSearchRepository(
    context: Context,
    private val weekly: WeeklyRepository
) {
    private class Cached(val at: Long, val data: Map<String, List<DailyForecast>>)

    private val prefs =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mutex = Mutex()
    private val gate = Semaphore(MAX_PARALLEL)
    private val cache = HashMap<String, Cached>()

    @Volatile
    private var index: Map<String, List<String>> = loadIndex()

    suspend fun search(query: String): Result<List<PlaceForecast>> {
        val (counties, keyword) = interpret(query)

        // 索引已知且沒有任何鄉鎮符合的縣市就不用抓；索引沒有的縣市一律要抓
        val toFetch = counties.filter { county ->
            keyword.isEmpty() || index[county]?.any { it.contains(keyword) } != false
        }
        if (toFetch.isEmpty()) return Result.success(emptyList())

        val fetched = coroutineScope {
            toFetch.map { county -> async { county to countyData(county) } }.awaitAll()
        }
        val ok = fetched.mapNotNull { (county, result) ->
            result.getOrNull()?.let { county to it }
        }
        // 全部失敗才算失敗；只有部分縣市失敗時，回傳抓得到的部分
        if (ok.isEmpty()) {
            return Result.failure(
                fetched.firstNotNullOfOrNull { it.second.exceptionOrNull() }
                    ?: IOException("no data")
            )
        }

        val places = ok.flatMap { (county, districts) ->
            districts.entries
                .filter { (name, _) -> keyword.isEmpty() || name.contains(keyword) }
                .mapNotNull { (name, daily) ->
                    daily.firstOrNull()?.let { today ->
                        PlaceForecast(
                            county = county,
                            district = name,
                            condition = today.condition,
                            minTemp = today.minTemp,
                            maxTemp = today.maxTemp,
                            rainChance = today.rainChance
                        )
                    }
                }
        }
        // 名稱完全相同的排最前面（排序是穩定的，其餘維持縣市順序）
        return Result.success(places.sortedBy { if (it.district == keyword) 0 else 1 }.take(MAX_RESULTS))
    }

    /** 把輸入拆成「候選縣市」與「鄉鎮關鍵字」。 */
    private fun interpret(query: String): Pair<List<String>, String> {
        val compact = query.replace('台', '臺').filterNot { it.isWhitespace() }
        val all = TaiwanRegions.allCounties()

        // 1. 完整縣市名開頭：「臺南市」「高雄市左營」
        all.firstOrNull { compact.startsWith(it) }?.let {
            return listOf(it) to compact.removePrefix(it)
        }

        // 2. 省略「市 / 縣」的縣市名開頭：「臺南」「高雄 左營」；「新竹」「嘉義」會同時符合縣與市
        val shorts = all.filter { compact.startsWith(it.dropLast(1)) }
        if (shorts.isNotEmpty()) {
            val rest = compact.removePrefix(shorts.first().dropLast(1))
            // 「花蓮市」「桃園區」：剩下的只是一個行政區後綴，代表整串其實是鄉鎮名
            if (rest !in GENERIC_SUFFIXES) return shorts to rest
        }

        // 3. 只有鄉鎮名：搜尋所有縣市
        return all to compact
    }

    private suspend fun countyData(county: String): Result<Map<String, List<DailyForecast>>> {
        val now = System.currentTimeMillis()
        mutex.withLock {
            cache[county]?.takeIf { now - it.at < CACHE_TTL_MS }
                ?.let { return Result.success(it.data) }
        }
        val result = gate.withPermit { weekly.getCountyWeekly(county) }
        result.getOrNull()?.let { data ->
            mutex.withLock {
                cache[county] = Cached(now, data)
                saveIndex(county, data.keys.toList())
            }
        }
        return result
    }

    private fun saveIndex(county: String, districts: List<String>) {
        if (index[county] == districts) return
        val updated = index + (county to districts)
        index = updated
        val json = JSONObject()
        updated.forEach { (name, list) -> json.put(name, JSONArray(list)) }
        prefs.edit().putString(KEY_INDEX, json.toString()).apply()
    }

    private fun loadIndex(): Map<String, List<String>> = runCatching {
        val json = JSONObject(prefs.getString(KEY_INDEX, "{}") ?: "{}")
        json.keys().asSequence().associateWith { county ->
            val array = json.getJSONArray(county)
            List(array.length()) { array.getString(it) }
        }
    }.getOrDefault(emptyMap())
}
