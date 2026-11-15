package com.artchknd.tryit.weather.global

import java.util.Locale

object WmoCodes {

    fun describe(code: Int, locale: Locale = Locale.getDefault()): String {
        val zh = locale.language == "zh"
        return when (code) {
            0 -> if (zh) "晴" else "Clear"
            1 -> if (zh) "大致晴朗" else "Mainly clear"
            2 -> if (zh) "多雲時晴" else "Partly cloudy"
            3 -> if (zh) "陰" else "Overcast"
            45, 48 -> if (zh) "霧" else "Fog"
            51, 53, 55 -> if (zh) "毛毛雨" else "Drizzle"
            56, 57 -> if (zh) "凍毛毛雨" else "Freezing drizzle"
            61 -> if (zh) "小雨" else "Light rain"
            63 -> if (zh) "中雨" else "Rain"
            65 -> if (zh) "大雨" else "Heavy rain"
            66, 67 -> if (zh) "凍雨" else "Freezing rain"
            71 -> if (zh) "小雪" else "Light snow"
            73 -> if (zh) "中雪" else "Snow"
            75 -> if (zh) "大雪" else "Heavy snow"
            77 -> if (zh) "雪粒" else "Snow grains"
            80 -> if (zh) "陣雨" else "Rain showers"
            81 -> if (zh) "較強陣雨" else "Heavy showers"
            82 -> if (zh) "劇烈陣雨" else "Violent showers"
            85, 86 -> if (zh) "陣雪" else "Snow showers"
            95 -> if (zh) "雷雨" else "Thunderstorm"
            96, 99 -> if (zh) "雷雨伴冰雹" else "Thunderstorm with hail"
            else -> if (zh) "未知" else "Unknown"
        }
    }
}