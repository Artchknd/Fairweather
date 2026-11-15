package com.artchknd.tryit.ui.components

import android.content.Context
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.artchknd.tryit.weather.DailyForecast
import com.artchknd.tryit.weather.WeatherText
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun WeeklyForecastSection(
    daily: List<DailyForecast>,
    todayMinTemp: Int?,
    todayMaxTemp: Int?,
    modifier: Modifier = Modifier
) {
    if (daily.isEmpty()) return

    val colors = FairweatherTheme.colors
    val context = LocalContext.current
    val today = LocalDate.now()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = stringResource(R.string.weather_weekly_forecast_title),
            color = colors.textMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp
        )

        HorizontalDivider(
            color = colors.textPrimary.copy(alpha = 0.16f),
            thickness = 0.8.dp
        )

        daily.forEachIndexed { index, item ->
            val parsedDate = parseLocalDateOrNull(item.date)
            val isToday = parsedDate != null && parsedDate == today
            val useTodayHeaderTemps = index == 0 && isToday

            WeeklyForecastRow(
                item = item,
                dayLabel = formatWeeklyDayLabel(context, item.date, parsedDate, isToday),
                isToday = isToday,
                displayMinTemp = if (useTodayHeaderTemps) todayMinTemp else item.minTemp,
                displayMaxTemp = if (useTodayHeaderTemps) todayMaxTemp else item.maxTemp
            )

            if (index < daily.lastIndex) {
                HorizontalDivider(
                    color = colors.textPrimary.copy(alpha = 0.08f),
                    thickness = 0.6.dp
                )
            }
        }
    }
}

@Composable
private fun WeeklyForecastRow(
    item: DailyForecast,
    dayLabel: String,
    isToday: Boolean,
    displayMinTemp: Int?,
    displayMaxTemp: Int?
) {
    val colors = FairweatherTheme.colors
    val context = LocalContext.current
    val rainText = item.rainChance?.let { "$it%" } ?: "--"
    val minStr = displayMinTemp?.toString() ?: "--"
    val maxStr = displayMaxTemp?.toString() ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.width(48.dp)
        )

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WeatherIcon(
                condition = item.condition,
                tint = colors.iconTint,
                modifier = Modifier.size(22.dp),
                conditionCode = item.conditionCode,
                wmoCode = item.wmoCode,
                isDay = true
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = WeatherText.condition(context, item.conditionCode, item.condition)
                    .ifBlank { "--" },
                color = colors.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .basicMarquee(
                        iterations = Int.MAX_VALUE,
                        repeatDelayMillis = 1600,
                        initialDelayMillis = 1000,
                        velocity = 30.dp
                    )
            )
        }

        Text(
            text = rainText,
            color = colors.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(38.dp)
        )

        Box(
            modifier = Modifier.width(46.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (item.uvIndex != null) {
                Text(
                    text = "UV ${item.uvIndex}",
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        Text(
            text = "${minStr}° / ${maxStr}°",
            color = colors.textPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.width(76.dp)
        )
    }
}

private fun parseLocalDateOrNull(dateText: String): LocalDate? {
    return try {
        LocalDate.parse(dateText)
    } catch (_: Exception) {
        null
    }
}

private fun formatWeeklyDayLabel(
    context: Context,
    rawDate: String,
    parsedDate: LocalDate?,
    isToday: Boolean
): String {
    if (isToday) return context.getString(R.string.day_label_today)
    if (parsedDate == null) return rawDate
    val res = when (parsedDate.dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.day_label_mon
        DayOfWeek.TUESDAY -> R.string.day_label_tue
        DayOfWeek.WEDNESDAY -> R.string.day_label_wed
        DayOfWeek.THURSDAY -> R.string.day_label_thu
        DayOfWeek.FRIDAY -> R.string.day_label_fri
        DayOfWeek.SATURDAY -> R.string.day_label_sat
        DayOfWeek.SUNDAY -> R.string.day_label_sun
    }
    return context.getString(res)
}