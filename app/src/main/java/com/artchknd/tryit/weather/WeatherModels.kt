package com.artchknd.tryit.weather

data class ForecastSlot(
    val start: String,
    val end: String,
    val condition: String,
    val rainChance: Int?,
    val minTemp: Int?,
    val maxTemp: Int?,
    val comfort: String,
    val conditionCode: Int? = null,   // CWA WeatherCode（台灣資料）
    val wmoCode: Int? = null          // WMO 天氣代碼（境外資料）
)

enum class LocationStatus {
    OK,
    PERMISSION_REQUIRED,
    UNSUPPORTED_REGION   // 全球資料層加入後不會再產生，保留是為了不動到既有的畫面
}

/** 這份天氣資料的來源，畫面用它決定圖示對照與資料來源標示。 */
enum class DataSource {
    TAIWAN_CWA,
    OPEN_METEO
}

data class WeatherUiState(
    val city: String = "",
    val district: String = "",         // 境外時是「行政區, 國家」的次要標示
    val tempNow: Int? = null,          // 台灣目前沒有；境外有值
    val condition: String = "",
    val rainChance: Int? = null,
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val slots: List<ForecastSlot> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val daily: List<DailyForecast> = emptyList(),
    val locationStatus: LocationStatus = LocationStatus.OK,
    val conditionCode: Int? = null,    // CWA WeatherCode（台灣資料）
    val wmoCode: Int? = null,          // WMO 天氣代碼（境外資料）
    val isDay: Boolean = true,         // 境外資料才會是 false（夜間）
    val feelsLike: Int? = null,        // 境外資料才有
    val source: DataSource = DataSource.TAIWAN_CWA
)