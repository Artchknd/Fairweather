package com.artchknd.tryit.weather

/**
 * 一個固定地區的識別，也是持久化的內容。
 * 台灣：縣市 + 鄉鎮，沒有座標。
 * 境外：county 放地名、district 放「行政區, 國家」標示，並帶座標。
 */
data class PlaceKey(
    val county: String,
    val district: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val isGlobal: Boolean get() = latitude != null && longitude != null
}

/** 某個地區「今天」的摘要，搜尋結果與固定清單共用。 */
data class PlaceForecast(
    val county: String,
    val district: String,
    val condition: String,
    val minTemp: Int?,
    val maxTemp: Int?,
    val rainChance: Int?,
    val latitude: Double? = null,
    val longitude: Double? = null
) {
    val id: String get() = "$county|$district"
    val key: PlaceKey get() = PlaceKey(county, district, latitude, longitude)

    companion object {
        /** 預報尚未載入時的占位項目。 */
        fun placeholder(key: PlaceKey) = PlaceForecast(
            county = key.county,
            district = key.district,
            condition = "--",
            minTemp = null,
            maxTemp = null,
            rainChance = null,
            latitude = key.latitude,
            longitude = key.longitude
        )
    }
}

data class SearchUiState(
    val results: List<PlaceForecast> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)