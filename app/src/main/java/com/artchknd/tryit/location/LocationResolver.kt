package com.artchknd.tryit.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.SystemClock
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.cos

sealed interface LocationResult {
    data class Success(val county: String, val district: String) : LocationResult
    // 不在台灣：帶著座標交給全球資料層
    data class OutsideTaiwan(val latitude: Double, val longitude: Double) : LocationResult
    data object PermissionDenied : LocationResult
    data object LocationUnavailable : LocationResult
    data object LookupFailed : LocationResult
}

private sealed interface PointResult {
    data class Found(val county: String, val district: String) : PointResult
    data object NotFound : PointResult
    data object Failed : PointResult
}

class LocationResolver(context: Context) {

    private val appContext = context.applicationContext

    suspend fun resolve(): LocationResult {
        val hasFine = isGranted(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarse = isGranted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasFine && !hasCoarse) return LocationResult.PermissionDenied

        val location = fetchLocation(hasFine) ?: return LocationResult.LocationUnavailable
        if (!isInsideTaiwanBounds(location.latitude, location.longitude)) {
            return LocationResult.OutsideTaiwan(location.latitude, location.longitude)
        }
        return lookupAdministrativeArea(location.latitude, location.longitude)
    }

    private fun isGranted(permission: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

    // 粗略範圍（含澎湖、金門、馬祖、蘭嶼），只用來略過明顯的外國位置。
    // 範圍內仍由 NLSC 做最終判定。
    private fun isInsideTaiwanBounds(lat: Double, lon: Double): Boolean =
        lat in MIN_LAT..MAX_LAT && lon in MIN_LON..MAX_LON

    @SuppressLint("MissingPermission")
    private suspend fun fetchLocation(hasFine: Boolean): Location? {
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null

        val providers = buildList {
            add(LocationManager.NETWORK_PROVIDER)
            if (hasFine) add(LocationManager.GPS_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }

        for (provider in providers) {
            val fresh = withTimeoutOrNull(CURRENT_LOCATION_TIMEOUT_MS) {
                currentLocation(manager, provider)
            }
            if (fresh != null) return fresh
        }

        return providers
            .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { ageMillis(it) <= MAX_LAST_KNOWN_AGE_MS }
            .maxByOrNull { it.elapsedRealtimeNanos }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                manager.getCurrentLocation(
                    provider,
                    signal,
                    ContextCompat.getMainExecutor(appContext)
                ) { location ->
                    if (cont.isActive) cont.resume(location)
                }
            } catch (e: SecurityException) {
                if (cont.isActive) cont.resume(null)
            }
        }

    private fun ageMillis(location: Location): Long =
        (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L

    private suspend fun lookupAdministrativeArea(lat: Double, lon: Double): LocationResult {
        when (val center = queryPoint(lat, lon)) {
            is PointResult.Found -> return LocationResult.Success(center.county, center.district)
            PointResult.Failed -> return LocationResult.LookupFailed
            PointResult.NotFound -> Unit
        }

        // 中心點查無：可能在台灣的海上或港口，也可能在外國。
        // 往周圍探測，任何一點命中行政區就視為台灣。
        val nearby = queryNearby(lat, lon)
        val found = nearby.filterIsInstance<PointResult.Found>().firstOrNull()
        return when {
            found != null -> LocationResult.Success(found.county, found.district)
            nearby.any { it is PointResult.Failed } -> LocationResult.LookupFailed
            else -> LocationResult.OutsideTaiwan(lat, lon)
        }
    }

    private suspend fun queryNearby(lat: Double, lon: Double): List<PointResult> = coroutineScope {
        val dLat = NEARBY_RADIUS_KM / KM_PER_DEGREE
        val dLon = dLat / cos(Math.toRadians(lat))
        NEARBY_DIRECTIONS.map { (north, east) ->
            async { queryPoint(lat + north * dLat, lon + east * dLon) }
        }.awaitAll()
    }

    private suspend fun queryPoint(lat: Double, lon: Double): PointResult =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                // 座標四捨五入到小數 3 位（約 110 公尺），不把精確位置送出去
                val url = URL(String.format(Locale.US, NLSC_URL_FORMAT, lon, lat))
                connection = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = NETWORK_TIMEOUT_MS
                    readTimeout = NETWORK_TIMEOUT_MS
                    setRequestProperty("User-Agent", "Fairweather/1.0")
                }
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext PointResult.Failed
                }
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val rawCounty = xmlTag(body, "ctyName")
                val rawTown = xmlTag(body, "townName")
                if (rawCounty == null || rawTown == null) {
                    return@withContext PointResult.NotFound
                }
                val county = TaiwanRegions.normalizeCounty(rawCounty)
                    ?: return@withContext PointResult.Failed
                PointResult.Found(county, TaiwanRegions.normalizeDistrict(rawTown))
            } catch (e: IOException) {
                PointResult.Failed
            } finally {
                connection?.disconnect()
            }
        }

    private fun xmlTag(body: String, tag: String): String? =
        Regex("<$tag>([^<]+)</$tag>").find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }

    private companion object {
        const val MIN_LAT = 21.8
        const val MAX_LAT = 26.5
        const val MIN_LON = 118.0
        const val MAX_LON = 122.2
        const val CURRENT_LOCATION_TIMEOUT_MS = 8_000L
        const val MAX_LAST_KNOWN_AGE_MS = 6 * 60 * 60 * 1000L
        const val NETWORK_TIMEOUT_MS = 8_000
        const val NEARBY_RADIUS_KM = 1.5
        const val KM_PER_DEGREE = 111.0
        const val NLSC_URL_FORMAT =
            "https://api.nlsc.gov.tw/other/TownVillagePointQuery1/%.3f/%.3f/4326"

        // (北向分量, 東向分量)：8 個方向
        val NEARBY_DIRECTIONS = listOf(
            1.0 to 0.0, -1.0 to 0.0, 0.0 to 1.0, 0.0 to -1.0,
            0.7 to 0.7, 0.7 to -0.7, -0.7 to 0.7, -0.7 to -0.7
        )
    }
}