package com.artchknd.tryit.weather.global

data class GlobalPlace(
    val name: String,
    val admin: String?,
    val country: String?,
    val countryCode: String?,
    val latitude: Double,
    val longitude: Double
) {
    /** 次要標示，例如 "Texas, United States"；什麼都不知道時為空字串。 */
    val label: String
        get() = listOfNotNull(
            admin?.takeIf { it != name },
            country?.takeIf { it != name }
        ).joinToString(", ")
}

data class GlobalCurrent(
    val time: String,
    val temp: Double,
    val feelsLike: Double,
    val code: Int,
    val isDay: Boolean,
    val humidity: Int?,
    val windKmh: Double?
)

data class GlobalSlot(
    val start: String,
    val end: String,
    val code: Int,
    val rainChance: Int?,
    val minTemp: Double,
    val maxTemp: Double,
    val feelsLikeAvg: Double
)

data class GlobalDay(
    val date: String,
    val code: Int,
    val rainChance: Int?,
    val minTemp: Double,
    val maxTemp: Double,
    val uvIndex: Int?
)

data class GlobalWeather(
    val current: GlobalCurrent,
    val slots: List<GlobalSlot>,
    val days: List<GlobalDay>
)

/** 某地「今天」的摘要，搜尋結果與固定清單用。 */
data class GlobalToday(
    val code: Int,
    val minTemp: Double,
    val maxTemp: Double,
    val rainChance: Int?
)