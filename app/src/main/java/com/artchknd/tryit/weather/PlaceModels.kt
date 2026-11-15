package com.artchknd.tryit.weather

/** 一個固定地區的識別（縣市 + 鄉鎮），也是持久化的內容。 */
data class PlaceKey(val county: String, val district: String)

/** 某個鄉鎮「今天」的摘要，搜尋結果與固定清單共用。 */
data class PlaceForecast(
    val county: String,
    val district: String,
    val condition: String,
    val minTemp: Int?,
    val maxTemp: Int?,
    val rainChance: Int?
) {
    val id: String get() = "$county|$district"
    val key: PlaceKey get() = PlaceKey(county, district)

    companion object {
        /** 預報尚未載入時的占位項目。 */
        fun placeholder(key: PlaceKey) = PlaceForecast(
            county = key.county,
            district = key.district,
            condition = "--",
            minTemp = null,
            maxTemp = null,
            rainChance = null
        )
    }
}

data class SearchUiState(
    val results: List<PlaceForecast> = emptyList(),
    val isSearching: Boolean = false,
    val error: String? = null
)
