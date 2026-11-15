package com.artchknd.tryit.weather.global

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

class GlobalGeocoder(private val context: Context) {

    suspend fun reverse(
        latitude: Double,
        longitude: Double,
        locale: Locale = Locale.getDefault()
    ): GlobalPlace? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            @Suppress("DEPRECATION")
            val list = Geocoder(context, locale).getFromLocation(latitude, longitude, 1)
            val a = list?.firstOrNull() ?: return@runCatching null
            val name = a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName
            ?: return@runCatching null
            GlobalPlace(
                name = name,
                admin = a.adminArea,
                country = a.countryName,
                countryCode = a.countryCode,
                latitude = latitude,
                longitude = longitude
            )
        }.getOrNull()
    }
}