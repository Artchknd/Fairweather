package com.artchknd.tryit.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.R
import com.artchknd.tryit.ui.theme.FairweatherTheme
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

enum class MainTab(@StringRes val labelResId: Int) {
    HOME(R.string.nav_tab_home),
    PINNED(R.string.nav_tab_pinned),
    SEARCH(R.string.nav_tab_search),
    SETTINGS(R.string.nav_tab_settings)
}

@Composable
fun FloatingNavBar(
    selectedTab: MainTab,
    onSelectTab: (MainTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors

    BoxWithConstraints(
        modifier = modifier
            .offset(y = 6.dp) // 整個導航欄往下移（可自行微調數值）
            .fillMaxWidth()
            .height(60.dp)
            .liquidGlass(shape = { CircleShape })
            .padding(horizontal = 10.dp)
    ) {
        val density = LocalDensity.current
        val tabCount = MainTab.entries.size
        val tabWidthPx = with(density) { (maxWidth / tabCount).toPx() }
        val selectedIndex = MainTab.entries.indexOf(selectedTab).toFloat()

        // 指示條滑動進度（維持原本彈簧阻尼參數）
        val animatedIndex by animateFloatAsState(
            targetValue = selectedIndex,
            animationSpec = spring(
                dampingRatio = 0.72f,
                stiffness = 340f,
                visibilityThreshold = 0.001f
            ),
            label = "navIndicatorIndex"
        )

        // 滑動中段產生流體拉伸感，停在任一分頁整數位置時自動收斂回原寬度
        val frac = ((animatedIndex % 1f) + 1f) % 1f
        val stretchFactor = sin(frac * PI).toFloat().coerceAtLeast(0f)
        val indicatorWidth = 22.dp + 16.dp * stretchFactor
        val indicatorWidthPx = with(density) { indicatorWidth.toPx() }
        val indicatorOffsetX = (tabWidthPx * (animatedIndex + 0.5f) - indicatorWidthPx / 2f).roundToInt()

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MainTab.entries.forEach { tab ->
                FloatingNavItem(
                    label = stringResource(tab.labelResId),
                    selected = tab == selectedTab,
                    onClick = { onSelectTab(tab) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 9.dp)
                .offset { IntOffset(x = indicatorOffsetX, y = 0) }
                .width(indicatorWidth)
                .height(3.dp)
                .clip(CircleShape)
                .background(colors.textPrimary)
        )
    }
}

@Composable
private fun FloatingNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val textColor by animateColorAsState(
        targetValue = if (selected) colors.textPrimary else colors.textMuted,
        animationSpec = tween(durationMillis = 200),
        label = "navItemTextColor"
    )
    val scale by animateFloatAsState(
        targetValue = when {
            pressed -> 0.92f
            selected -> 1.03f
            else -> 0.97f
        },
        animationSpec = spring(
            dampingRatio = 0.65f,
            stiffness = 480f
        ),
        label = "navItemScale"
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(bottom = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
        )
    }
}