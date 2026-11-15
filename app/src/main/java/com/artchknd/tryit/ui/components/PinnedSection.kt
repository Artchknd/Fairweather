package com.artchknd.tryit.ui.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme

@Composable
fun PinnedSection(
    pinnedItems: List<LocationSummaryItem>,
    onUnpin: (LocationSummaryItem) -> Unit,
    onGoToSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.pinned_section_title),
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        if (pinnedItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 96.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pinned_empty_title),
                        color = colors.textPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.pinned_empty_desc),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    GlassButton(
                        text = stringResource(R.string.pinned_empty_action),
                        onClick = onGoToSearch
                    )
                }
            }
        } else {
            Text(
                text = stringResource(R.string.pinned_items_count, pinnedItems.size),
                color = colors.textMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            )

            HorizontalDivider(
                color = colors.textPrimary.copy(alpha = 0.16f),
                thickness = 0.8.dp
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                pinnedItems.forEachIndexed { index, item ->
                    PinnedLocationRow(
                        item = item,
                        onUnpin = { onUnpin(item) }
                    )
                    if (index < pinnedItems.lastIndex) {
                        HorizontalDivider(
                            color = colors.textPrimary.copy(alpha = 0.08f),
                            thickness = 0.6.dp
                        )
                    }
                }

                Spacer(Modifier.height(116.dp))
            }
        }
    }
}

@Composable
private fun PinnedLocationRow(
    item: LocationSummaryItem,
    onUnpin: () -> Unit
) {
    val colors = FairweatherTheme.colors
    val minStr = item.minTemp?.toString() ?: "--"
    val maxStr = item.maxTemp?.toString() ?: "--"
    val rainStr = item.rainChance?.toString() ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.displayName,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                WeatherIcon(
                    condition = item.condition,
                    tint = colors.iconTint,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = item.condition.ifBlank { "--" },
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .basicMarquee(
                            iterations = Int.MAX_VALUE,
                            repeatDelayMillis = 1600,
                            initialDelayMillis = 1000,
                            velocity = 30.dp
                        )
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.weather_rain_prob_short, rainStr),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        Text(
            text = "${minStr}° / ${maxStr}°",
            color = colors.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(end = 12.dp)
        )

        GlassButton(
            text = stringResource(R.string.action_remove),
            onClick = onUnpin
        )
    }
}