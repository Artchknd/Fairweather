package com.artchknd.tryit.ui.components

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.artchknd.tryit.weather.ForecastSlot
import com.artchknd.tryit.weather.WeatherText
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun ForecastSection(
    slots: List<ForecastSlot>,
    modifier: Modifier = Modifier
) {
    if (slots.isEmpty()) return

    val colors = FairweatherTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.forecast_title_36h),
            color = colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )

        HorizontalDivider(
            color = colors.textPrimary.copy(alpha = 0.16f),
            thickness = 0.8.dp
        )

        slots.forEachIndexed { index, slot ->
            ForecastRow(slot = slot)
            if (index < slots.lastIndex) {
                HorizontalDivider(
                    color = colors.textPrimary.copy(alpha = 0.08f),
                    thickness = 0.6.dp
                )
            }
        }
    }
}

@Composable
private fun ForecastRow(slot: ForecastSlot) {
    val colors = FairweatherTheme.colors
    val context = LocalContext.current
    val (periodLabel, timeRange) = formatForecastTime(context, slot.start, slot.end)
    val minStr = slot.minTemp?.toString() ?: "--"
    val maxStr = slot.maxTemp?.toString() ?: "--"
    val rainStr = slot.rainChance?.toString() ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.1f)) {
            Text(
                text = periodLabel,
                color = colors.textPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = timeRange,
                color = colors.textMuted,
                fontSize = 12.sp
            )
        }

        Row(
            modifier = Modifier.weight(1.4f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIcon(
                condition = slot.condition,
                tint = colors.iconTint,
                modifier = Modifier.size(24.dp),
                conditionCode = slot.conditionCode,
                wmoCode = slot.wmoCode,
                isDay = true
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = WeatherText.condition(context, slot.conditionCode, slot.condition)
                        .ifBlank { "--" },
                    color = colors.textPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                val rainLabel = stringResource(R.string.forecast_rain_label, rainStr)
                val subInfo = buildString {
                    append(rainLabel)
                    val comfortText = WeatherText.comfort(context, slot.comfort)
                    if (comfortText.isNotBlank()) append(" · $comfortText")
                }
                Text(
                    text = subInfo,
                    color = colors.textMuted,
                    fontSize = 12.sp
                )
            }
        }

        Text(
            text = "${minStr}° / ${maxStr}°",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun formatForecastTime(context: Context, start: String, end: String): Pair<String, String> {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val startDt = LocalDateTime.parse(start, formatter)
        val endDt = LocalDateTime.parse(end, formatter)
        val today = LocalDate.now()
        val dayDiff = ChronoUnit.DAYS.between(today, startDt.toLocalDate())

        val dayPrefix = when (dayDiff) {
            0L -> context.getString(R.string.period_day_today)
            1L -> context.getString(R.string.period_day_tomorrow)
            2L -> context.getString(R.string.period_day_after)
            else -> context.getString(
                R.string.period_day_date,
                startDt.monthValue,
                startDt.dayOfMonth
            )
        }

        val period = when (startDt.hour) {
            in 0..5 -> context.getString(R.string.period_dawn, dayPrefix)
            in 6..11 -> if (endDt.hour == 18) {
                context.getString(R.string.period_daytime, dayPrefix)
            } else {
                context.getString(R.string.period_morning, dayPrefix)
            }
            in 12..17 -> context.getString(R.string.period_afternoon, dayPrefix)
            else -> context.getString(R.string.period_evening, dayPrefix)
        }

        val rangeStr = "%02d:%02d - %02d:%02d".format(
            startDt.hour, startDt.minute,
            endDt.hour, endDt.minute
        )
        period to rangeStr
    } catch (_: Exception) {
        val safeStart = start.substringAfter(' ', start).take(5)
        val safeEnd = end.substringAfter(' ', end).take(5)
        context.getString(R.string.period_fallback) to "$safeStart - $safeEnd"
    }
}