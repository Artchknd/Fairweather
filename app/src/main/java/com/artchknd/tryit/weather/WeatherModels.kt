package com.artchknd.tryit.weather

data class ForecastSlot(
    val start: String,
    val end: String,
    val condition: String,
    val rainChance: Int?,
    val minTemp: Int?,
    val maxTemp: Int?,
    val comfort: String,
    val conditionCode: Int? = null
)

enum class LocationStatus {
    OK,
    PERMISSION_REQUIRED,
    UNSUPPORTED_REGION
}

data class WeatherUiState(
    val city: String = "",
    val district: String = "",
    val tempNow: Int? = null,          // 之後接觀測資料才會有值
    val condition: String = "",
    val rainChance: Int? = null,
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val slots: List<ForecastSlot> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val daily: List<DailyForecast> = emptyList(),
    val locationStatus: LocationStatus = LocationStatus.OK,
    val conditionCode: Int? = null
)