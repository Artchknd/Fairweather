package com.artchknd.tryit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme

@Composable
fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    results: List<LocationSummaryItem>,
    pinnedIds: Set<String>,
    onTogglePin: (LocationSummaryItem) -> Unit,
    isSearching: Boolean,
    searchError: String?,
    hasSearched: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.search_section_title),
            color = colors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        // 頂部固定搜尋列：左側液態玻璃輸入框 + 右側獨立圓形放大鏡按鈕
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .liquidGlass(shape = { CircleShape })
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = stringResource(R.string.search_input_placeholder),
                                color = colors.textMuted,
                                fontSize = 15.sp,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            singleLine = true,
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(colors.textPrimary),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    focusManager.clearFocus()
                                    onSearch(query)
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (query.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.search_action_clear),
                            color = colors.textMuted,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onQueryChange("") }
                                )
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            GlassIconButton(
                onClick = {
                    focusManager.clearFocus()
                    onSearch(query)
                },
                size = 52.dp,
                enabled = !isSearching
            ) {
                SearchMagnifierIcon(
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 搜尋結果區（純排版，無玻璃框包字）
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when {
                isSearching -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = colors.textPrimary,
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = stringResource(R.string.search_loading_status),
                            color = colors.textSecondary,
                            fontSize = 14.sp
                        )
                    }
                }

                searchError != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 36.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = searchError,
                            color = colors.errorAccent,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        GlassButton(
                            text = stringResource(R.string.action_retry),
                            onClick = { onSearch(query) }
                        )
                    }
                }

                results.isNotEmpty() -> {
                    Text(
                        text = stringResource(R.string.search_results_header),
                        color = colors.textMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.2.sp
                    )
                    HorizontalDivider(
                        color = colors.textPrimary.copy(alpha = 0.16f),
                        thickness = 0.8.dp
                    )

                    results.forEachIndexed { index, item ->
                        val isPinned = item.id in pinnedIds
                        SearchResultRow(
                            item = item,
                            isPinned = isPinned,
                            onTogglePin = { onTogglePin(item) }
                        )
                        if (index < results.lastIndex) {
                            HorizontalDivider(
                                color = colors.textPrimary.copy(alpha = 0.08f),
                                thickness = 0.6.dp
                            )
                        }
                    }
                }

                hasSearched -> {
                    Text(
                        text = stringResource(R.string.search_empty_result, query.trim()),
                        color = colors.textSecondary,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 24.dp)
                    )
                }

                else -> {
                    Text(
                        text = stringResource(R.string.search_initial_hint),
                        color = colors.textMuted,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(116.dp))
        }
    }
}

@Composable
private fun SearchResultRow(
    item: LocationSummaryItem,
    isPinned: Boolean,
    onTogglePin: () -> Unit
) {
    val colors = FairweatherTheme.colors
    val minStr = item.minTemp?.toString() ?: "--"
    val maxStr = item.maxTemp?.toString() ?: "--"
    val rainStr = item.rainChance?.toString() ?: "--"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = item.displayName,
                color = colors.textPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.condition.isNotBlank()) {
                    WeatherIcon(
                        condition = item.condition,
                        tint = colors.iconTint,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = item.condition,
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
                }
                Text(
                    text = stringResource(
                        R.string.weather_compact_temp_rain,
                        minStr,
                        maxStr,
                        rainStr
                    ),
                    color = colors.textMuted,
                    fontSize = 12.sp,
                    maxLines = 1
                )
            }
        }

        GlassButton(
            text = if (isPinned) stringResource(R.string.action_pinned) else stringResource(R.string.action_pin),
            onClick = onTogglePin
        )
    }
}

@Composable
private fun SearchMagnifierIcon(
    tint: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.11f
        val radius = size.minDimension * 0.30f
        val center = Offset(size.width * 0.42f, size.height * 0.42f)
        drawCircle(
            color = tint,
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )
        val startOffset = radius * 0.72f
        drawLine(
            color = tint,
            start = Offset(center.x + startOffset, center.y + startOffset),
            end = Offset(size.width * 0.86f, size.height * 0.86f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}