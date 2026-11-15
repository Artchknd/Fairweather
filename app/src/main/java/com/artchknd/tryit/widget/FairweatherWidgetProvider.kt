package com.artchknd.tryit.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.view.View
import android.widget.RemoteViews
import com.artchknd.tryit.MainActivity
import com.artchknd.tryit.R
import com.artchknd.tryit.location.LocationResolver
import com.artchknd.tryit.location.LocationResult
import com.artchknd.tryit.weather.LocationStatus
import com.artchknd.tryit.weather.WeatherRepository
import com.artchknd.tryit.weather.WeatherText
import com.artchknd.tryit.weather.WeatherUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class FairweatherWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            renderWidget(context, appWidgetManager, appWidgetId, isRefreshing = false)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val pendingResult = goAsync()
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FairweatherWidgetProvider::class.java))
            ids.forEach { renderWidget(context, manager, it, isRefreshing = true) }

            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                try {
                    fetchAndSaveLatest(context)
                } finally {
                    val updatedIds = manager.getAppWidgetIds(
                        ComponentName(context, FairweatherWidgetProvider::class.java)
                    )
                    updatedIds.forEach { renderWidget(context, manager, it, isRefreshing = false) }
                    pendingResult.finish()
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.artchknd.tryit.widget.ACTION_REFRESH"

        fun syncFromApp(context: Context, state: WeatherUiState) {
            WidgetWeatherCache.saveFromUiState(context, state)
            updateAll(context)
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, FairweatherWidgetProvider::class.java))
            for (id in ids) {
                renderWidget(context, manager, id, isRefreshing = false)
            }
        }

        private suspend fun fetchAndSaveLatest(context: Context) {
            val cached = WidgetWeatherCache.load(context)
            val resolver = LocationResolver(context)
            val repo = WeatherRepository()

            var targetCity = ""
            var targetDistrict = ""

            when (val loc = resolver.resolve()) {
                is LocationResult.Success -> {
                    targetCity = loc.county
                    targetDistrict = loc.district
                }
                LocationResult.PermissionDenied -> {
                    WidgetWeatherCache.saveFromUiState(
                        context,
                        WeatherUiState(isLoading = false, locationStatus = LocationStatus.PERMISSION_REQUIRED)
                    )
                    return
                }
                is LocationResult.OutsideTaiwan -> {
                    if (cached != null && cached.city.isNotBlank()) {
                        targetCity = cached.city
                        targetDistrict = cached.district
                    }
                }
                LocationResult.LocationUnavailable, LocationResult.LookupFailed -> {
                    if (cached != null && cached.city.isNotBlank()) {
                        targetCity = cached.city
                        targetDistrict = cached.district
                    }
                }
            }

            if (targetCity.isBlank()) return

            repo.getForecast(targetCity).onSuccess { ui ->
                WidgetWeatherCache.saveFromUiState(
                    context,
                    ui.copy(district = targetDistrict, isLoading = false, locationStatus = LocationStatus.OK)
                )
            }
        }

        private fun renderWidget(
            context: Context,
            manager: AppWidgetManager,
            appWidgetId: Int,
            isRefreshing: Boolean
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_fairweather)
            val snapshot = WidgetWeatherCache.load(context)
            val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
            val iconColor = if (isDark) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt()
            val density = context.resources.displayMetrics.density

            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openPending = PendingIntent.getActivity(
                context,
                appWidgetId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openPending)

            val refreshIntent = Intent(context, FairweatherWidgetProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            val refreshPending = PendingIntent.getBroadcast(
                context,
                0,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_refresh_btn, refreshPending)
            views.setInt(R.id.widget_refresh_btn, "setColorFilter", iconColor)

            val updatedText = when {
                isRefreshing -> context.getString(R.string.widget_updating)
                snapshot != null && snapshot.updatedAtMillis > 0L -> {
                    val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                        .format(Date(snapshot.updatedAtMillis))
                    context.getString(R.string.widget_updated_at, timeStr)
                }
                else -> ""
            }
            views.setTextViewText(R.id.widget_updated, updatedText)

            when {
                snapshot == null -> {
                    views.setTextViewText(R.id.widget_location, context.getString(R.string.app_name))
                    views.setViewVisibility(R.id.widget_content_container, View.GONE)
                    views.setViewVisibility(R.id.widget_status_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_title, context.getString(R.string.widget_empty_title))
                    views.setTextViewText(R.id.widget_status_desc, context.getString(R.string.widget_empty_desc))
                }

                snapshot.locationStatus == LocationStatus.PERMISSION_REQUIRED -> {
                    views.setTextViewText(R.id.widget_location, context.getString(R.string.app_name))
                    views.setViewVisibility(R.id.widget_content_container, View.GONE)
                    views.setViewVisibility(R.id.widget_status_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_title, context.getString(R.string.location_permission_title))
                    views.setTextViewText(R.id.widget_status_desc, context.getString(R.string.widget_empty_title))
                }

                snapshot.slots.isEmpty() -> {
                    views.setTextViewText(R.id.widget_location, formatLocation(snapshot.city, snapshot.district))
                    views.setViewVisibility(R.id.widget_content_container, View.GONE)
                    views.setViewVisibility(R.id.widget_status_container, View.VISIBLE)
                    views.setTextViewText(R.id.widget_status_title, context.getString(R.string.widget_empty_title))
                    views.setTextViewText(R.id.widget_status_desc, context.getString(R.string.widget_empty_desc))
                }

                else -> {
                    views.setViewVisibility(R.id.widget_status_container, View.GONE)
                    views.setViewVisibility(R.id.widget_content_container, View.VISIBLE)

                    views.setTextViewText(
                        R.id.widget_location,
                        formatLocation(snapshot.city, snapshot.district)
                    )

                    val mainIconPx = (36 * density).toInt()
                    views.setImageViewBitmap(
                        R.id.widget_main_icon,
                        WidgetIconRenderer.render(
                            condition = snapshot.condition,
                            color = iconColor,
                            sizePx = mainIconPx,
                            conditionCode = snapshot.conditionCode,
                            wmoCode = snapshot.wmoCode,
                            isDay = snapshot.isDay
                        )
                    )

                    val minStr = snapshot.minTemp?.toString() ?: "--"
                    val maxStr = snapshot.maxTemp?.toString() ?: "--"
                    val rainStr = snapshot.rainChance?.toString() ?: "--"
                    val tempText = if (snapshot.tempNow != null) {
                        "${snapshot.tempNow}°"
                    } else {
                        "${minStr}° - ${maxStr}°"
                    }
                    views.setTextViewText(R.id.widget_main_temp, tempText)
                    views.setTextViewText(
                        R.id.widget_main_condition,
                        WeatherText.condition(context, snapshot.conditionCode, snapshot.condition)
                            .ifBlank { "--" }
                    )

                    val rawComfort = snapshot.slots.firstOrNull()?.comfort.orEmpty()
                    val comfort = if (rawComfort.isNotBlank()) {
                        WeatherText.comfort(context, rawComfort)
                    } else {
                        "--"
                    }
                    views.setTextViewText(
                        R.id.widget_main_detail,
                        context.getString(R.string.widget_range_subtitle, rainStr, comfort)
                    )

                    val slotViews = listOf(
                        SlotViewIds(R.id.widget_slot_0, R.id.widget_slot_0_time, R.id.widget_slot_0_icon, R.id.widget_slot_0_temp, R.id.widget_slot_0_rain),
                        SlotViewIds(R.id.widget_slot_1, R.id.widget_slot_1_time, R.id.widget_slot_1_icon, R.id.widget_slot_1_temp, R.id.widget_slot_1_rain),
                        SlotViewIds(R.id.widget_slot_2, R.id.widget_slot_2_time, R.id.widget_slot_2_icon, R.id.widget_slot_2_temp, R.id.widget_slot_2_rain)
                    )
                    val slotIconPx = (20 * density).toInt()

                    slotViews.forEachIndexed { index, ids ->
                        val slot = snapshot.slots.getOrNull(index)
                        if (slot == null) {
                            views.setViewVisibility(ids.container, View.INVISIBLE)
                        } else {
                            views.setViewVisibility(ids.container, View.VISIBLE)
                            views.setTextViewText(ids.time, formatPeriodLabel(context, slot.start, slot.end))
                            views.setImageViewBitmap(
                                ids.icon,
                                WidgetIconRenderer.render(
                                    condition = slot.condition,
                                    color = iconColor,
                                    sizePx = slotIconPx,
                                    conditionCode = slot.conditionCode,
                                    wmoCode = slot.wmoCode,
                                    isDay = true
                                )
                            )
                            val sMin = slot.minTemp?.toString() ?: "--"
                            val sMax = slot.maxTemp?.toString() ?: "--"
                            val sRain = slot.rainChance?.toString() ?: "--"
                            views.setTextViewText(ids.temp, "${sMin}°/${sMax}°")
                            views.setTextViewText(ids.rain, context.getString(R.string.weather_rain_prob_short, sRain))
                        }
                    }
                }
            }

            manager.updateAppWidget(appWidgetId, views)
        }

        private fun formatLocation(city: String, district: String): String {
            val c = city.trim()
            val d = district.trim()
            return when {
                c.isNotEmpty() && d.isNotEmpty() -> "$c $d"
                c.isNotEmpty() -> c
                d.isNotEmpty() -> d
                else -> "--"
            }
        }

        private fun formatPeriodLabel(context: Context, start: String, end: String): String {
            return try {
                val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val startDt = LocalDateTime.parse(start, fmt)
                val endDt = LocalDateTime.parse(end, fmt)
                val dayDiff = ChronoUnit.DAYS.between(LocalDate.now(), startDt.toLocalDate())
                val dayPrefix = when (dayDiff) {
                    0L -> context.getString(R.string.period_day_today)
                    1L -> context.getString(R.string.period_day_tomorrow)
                    2L -> context.getString(R.string.period_day_after)
                    else -> context.getString(R.string.period_day_date, startDt.monthValue, startDt.dayOfMonth)
                }
                when (startDt.hour) {
                    in 0..5 -> context.getString(R.string.period_dawn, dayPrefix)
                    in 6..11 -> {
                        if (endDt.hour == 18) context.getString(R.string.period_daytime, dayPrefix)
                        else context.getString(R.string.period_morning, dayPrefix)
                    }
                    in 12..17 -> context.getString(R.string.period_afternoon, dayPrefix)
                    else -> context.getString(R.string.period_evening, dayPrefix)
                }
            } catch (_: Exception) {
                context.getString(R.string.period_fallback)
            }
        }
    }

    private data class SlotViewIds(
        val container: Int,
        val time: Int,
        val icon: Int,
        val temp: Int,
        val rain: Int
    )
}