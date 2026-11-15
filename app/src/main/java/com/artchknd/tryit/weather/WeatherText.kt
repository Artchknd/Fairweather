package com.artchknd.tryit.weather

import android.content.Context
import com.artchknd.tryit.R
import com.artchknd.tryit.location.TaiwanRegions

/**
 * Localizes text that comes from the CWA API.
 *
 * Rule: in Chinese locales the original CWA text is shown (it is more
 * detailed than the category labels); in other locales the label is looked
 * up by code or key, and anything unknown falls back to the original text.
 */
object WeatherText {

    private val CONDITION_RES: Map<Int, Int> = mapOf(
        1 to R.string.wx_01,
        2 to R.string.wx_02,
        3 to R.string.wx_03,
        4 to R.string.wx_04,
        5 to R.string.wx_05,
        6 to R.string.wx_06,
        7 to R.string.wx_07,
        8 to R.string.wx_08,
        9 to R.string.wx_09,
        10 to R.string.wx_10,
        11 to R.string.wx_11,
        12 to R.string.wx_12,
        13 to R.string.wx_13,
        14 to R.string.wx_14,
        15 to R.string.wx_15,
        16 to R.string.wx_16,
        17 to R.string.wx_17,
        18 to R.string.wx_18,
        19 to R.string.wx_19,
        20 to R.string.wx_20,
        21 to R.string.wx_21,
        22 to R.string.wx_22,
        23 to R.string.wx_23,
        24 to R.string.wx_24,
        25 to R.string.wx_25,
        26 to R.string.wx_26,
        27 to R.string.wx_27,
        28 to R.string.wx_28,
        29 to R.string.wx_29,
        30 to R.string.wx_30,
        31 to R.string.wx_31,
        32 to R.string.wx_32,
        33 to R.string.wx_33,
        34 to R.string.wx_34,
        35 to R.string.wx_35,
        36 to R.string.wx_36,
        37 to R.string.wx_37,
        38 to R.string.wx_38,
        39 to R.string.wx_39,
        41 to R.string.wx_41,
        42 to R.string.wx_42,
    )

    private val COUNTY_RES: Map<String, Int> = mapOf(
        "宜蘭縣" to R.string.county_yilan,
        "桃園市" to R.string.county_taoyuan,
        "新竹縣" to R.string.county_hsinchu_county,
        "苗栗縣" to R.string.county_miaoli,
        "彰化縣" to R.string.county_changhua,
        "南投縣" to R.string.county_nantou,
        "雲林縣" to R.string.county_yunlin,
        "嘉義縣" to R.string.county_chiayi_county,
        "屏東縣" to R.string.county_pingtung,
        "臺東縣" to R.string.county_taitung,
        "花蓮縣" to R.string.county_hualien,
        "澎湖縣" to R.string.county_penghu,
        "基隆市" to R.string.county_keelung,
        "新竹市" to R.string.county_hsinchu_city,
        "嘉義市" to R.string.county_chiayi_city,
        "臺北市" to R.string.county_taipei,
        "高雄市" to R.string.county_kaohsiung,
        "新北市" to R.string.county_new_taipei,
        "臺中市" to R.string.county_taichung,
        "臺南市" to R.string.county_tainan,
        "連江縣" to R.string.county_lienchiang,
        "金門縣" to R.string.county_kinmen,
    )

    private val COMFORT_RES: Map<String, Int> = mapOf(
        "非常寒冷" to R.string.comfort_very_cold,
        "寒冷" to R.string.comfort_cold,
        "稍有寒意" to R.string.comfort_chilly,
        "舒適" to R.string.comfort_comfortable,
        "稍有悶熱" to R.string.comfort_slightly_muggy,
        "悶熱" to R.string.comfort_muggy,
        "易中暑" to R.string.comfort_heatstroke,
    )

    private fun isChinese(context: Context): Boolean =
        context.resources.configuration.locales[0].language == "zh"

    /** Weather condition. [code] is CWA's WeatherCode / parameterValue. */
    fun condition(context: Context, code: Int?, original: String): String {
        if (isChinese(context)) return original
        val res = CONDITION_RES[code] ?: return original
        return context.getString(res)
    }

    /** County or city name as returned by CWA (accepts both the 臺 and 台 spellings). */
    fun county(context: Context, name: String): String {
        if (isChinese(context)) return name
        val key = TaiwanRegions.normalizeCounty(name) ?: return name
        val res = COUNTY_RES[key] ?: return name
        return context.getString(res)
    }

    /** Comfort index text. Handles ranges such as "A至B"; unknown parts fall back to the original. */
    fun comfort(context: Context, text: String): String {
        if (text.isBlank() || isChinese(context)) return text
        val parts = text.split("至").map { it.trim() }
        val labels = parts.map { part ->
            val res = COMFORT_RES[part] ?: return text
            context.getString(res)
        }
        return when (labels.size) {
            1 -> labels[0]
            2 -> context.getString(R.string.comfort_range, labels[0], labels[1])
            else -> text
        }
    }
}