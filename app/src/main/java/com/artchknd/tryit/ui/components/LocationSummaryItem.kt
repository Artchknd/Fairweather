package com.artchknd.tryit.ui.components

import androidx.compose.runtime.Immutable

@Immutable
data class LocationSummaryItem(
    val city: String,
    val district: String = "",
    val condition: String = "",
    val minTemp: Int? = null,
    val maxTemp: Int? = null,
    val rainChance: Int? = null
) {
    val id: String
        get() {
            val cleanCity = city.trim()
            val cleanDistrict = district.trim()
            return if (cleanDistrict.isEmpty()) cleanCity else "$cleanCity|$cleanDistrict"
        }

    val displayName: String
        get() {
            val cleanCity = city.trim()
            val cleanDistrict = district.trim()
            return when {
                cleanCity.isNotEmpty() && cleanDistrict.isNotEmpty() -> "$cleanCity $cleanDistrict"
                cleanCity.isNotEmpty() -> cleanCity
                cleanDistrict.isNotEmpty() -> cleanDistrict
                else -> "--"
            }
        }
}