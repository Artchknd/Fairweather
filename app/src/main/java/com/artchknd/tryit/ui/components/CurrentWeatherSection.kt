package com.artchknd.tryit.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.artchknd.tryit.weather.WeatherText
import com.artchknd.tryit.weather.WeatherUiState

@Composable
fun CurrentWeatherHeader(
    city: String,
    district: String = "",
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    val cleanCity = city.trim().ifEmpty { "--" }
    val cleanDistrict = district.trim()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = cleanCity,
                    color = colors.textPrimary,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1600,
                            initialDelayMillis = 1000,
                            velocity = 36.dp
                        )
                )
                if (isLoading) {
                    Spacer(Modifier.width(10.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = colors.textPrimary,
                        strokeWidth = 2.dp
                    )
                }
            }

            if (cleanDistrict.isNotEmpty()) {
                Text(
                    text = cleanDistrict,
                    color = colors.textSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        GlassBadge(text = stringResource(R.string.developed_by))
    }
}

@Composable
fun CurrentWeatherSection(
    state: WeatherUiState,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    val context = LocalContext.current
    val minStr = state.minTemp?.toString() ?: "--"
    val maxStr = state.maxTemp?.toString() ?: "--"
    val rainStr = state.rainChance?.toString() ?: "--"
    val rawComfort = state.slots.firstOrNull()?.comfort.orEmpty()
    val comfort = if (rawComfort.isNotBlank()) WeatherText.comfort(context, rawComfort) else ""
    val conditionText = WeatherText.condition(context, state.conditionCode, state.condition)
        .ifBlank { "--" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeatherIcon(
                condition = state.condition,
                tint = colors.iconTint,
                modifier = Modifier.size(32.dp),
                conditionCode = state.conditionCode,
                wmoCode = state.wmoCode,
                isDay = state.isDay
            )
            Text(
                text = conditionText,
                color = colors.textSecondary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(Modifier.height(4.dp))

        if (state.tempNow != null) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${state.tempNow}°",
                    color = colors.textPrimary,
                    fontSize = 62.sp,
                    fontWeight = FontWeight.Bold
                )
                if (state.feelsLike != null) {
                    Text(
                        text = stringResource(R.string.weather_feels_like, state.feelsLike),
                        color = colors.textSecondary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }
            val summaryText = stringResource(
                R.string.weather_temp_rain_summary,
                maxStr,
                minStr,
                rainStr
            )
            val fullSummary = if (comfort.isNotBlank()) "$summaryText  ·  $comfort" else summaryText
            Text(
                text = fullSummary,
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        } else {
            Text(
                text = "${minStr}° - ${maxStr}°",
                color = colors.textPrimary,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            val rangeSubtitle = stringResource(R.string.weather_range_rain_subtitle, rainStr)
            val detailLine = if (comfort.isNotBlank()) {
                "$rangeSubtitle  ·  $comfort"
            } else {
                rangeSubtitle
            }
            Text(
                text = detailLine,
                color = colors.textSecondary,
                fontSize = 14.sp
            )
        }

        if (state.error != null) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(
                color = colors.errorAccent.copy(alpha = 0.35f),
                thickness = 0.8.dp
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.error,
                    color = colors.errorAccent,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                GlassButton(
                    text = stringResource(R.string.action_retry),
                    onClick = onRetry
                )
            }
        }
    }
}