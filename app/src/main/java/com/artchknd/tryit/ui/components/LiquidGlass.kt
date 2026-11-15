package com.artchknd.tryit.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.ui.theme.FairweatherTheme
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

object LiquidGlassDefaults {
    const val BLUR_DP = 3.04f
    const val LENS_HEIGHT_DP = 15.1f
    const val LENS_AMOUNT_DP = 25.79f
    const val SURFACE_ALPHA = 0.1f
    const val CORNER_DP = 28f
    const val VIBRANCY_ON = false
}

private val LocalLiquidBackdrop = staticCompositionLocalOf<Backdrop?> { null }

@Composable
fun ProvideLiquidBackdrop(
    backdrop: Backdrop,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLiquidBackdrop provides backdrop) {
        content()
    }
}

fun Modifier.liquidGlass(
    backdrop: Backdrop,
    shape: () -> Shape = { RoundedCornerShape(LiquidGlassDefaults.CORNER_DP.dp) },
    tint: Color = Color.White
): Modifier = drawBackdrop(
    backdrop = backdrop,
    shape = shape,
    effects = {
        if (LiquidGlassDefaults.VIBRANCY_ON) vibrancy()
        if (LiquidGlassDefaults.BLUR_DP > 0f) blur(LiquidGlassDefaults.BLUR_DP.dp.toPx())
        if (LiquidGlassDefaults.LENS_HEIGHT_DP > 0f && LiquidGlassDefaults.LENS_AMOUNT_DP > 0f) {
            lens(
                LiquidGlassDefaults.LENS_HEIGHT_DP.dp.toPx(),
                LiquidGlassDefaults.LENS_AMOUNT_DP.dp.toPx()
            )
        }
    },
    onDrawSurface = { drawRect(tint.copy(alpha = LiquidGlassDefaults.SURFACE_ALPHA)) }
)

@Composable
fun Modifier.liquidGlass(
    shape: () -> Shape = { RoundedCornerShape(LiquidGlassDefaults.CORNER_DP.dp) },
    tint: Color = FairweatherTheme.colors.glassTint
): Modifier {
    val backdrop = LocalLiquidBackdrop.current
    return if (backdrop != null) {
        liquidGlass(backdrop = backdrop, shape = shape, tint = tint)
    } else {
        this.background(tint.copy(alpha = LiquidGlassDefaults.SURFACE_ALPHA), shape())
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: () -> Shape = { RoundedCornerShape(LiquidGlassDefaults.CORNER_DP.dp) },
    tint: Color = FairweatherTheme.colors.glassTint,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .liquidGlass(shape = shape, tint = tint)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
fun GlassBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = FairweatherTheme.colors
    Box(
        modifier = modifier
            .liquidGlass(shape = { CircleShape }, tint = colors.glassTint)
            .height(36.dp)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colors.textSecondary,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1
        )
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = FairweatherTheme.colors
    GlassContentButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled
    ) {
        Text(
            text = text,
            color = colors.textPrimary,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

@Composable
fun GlassContentButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: () -> Shape = { CircleShape },
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = FairweatherTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.18f else 0f,
        animationSpec = tween(120),
        label = "glassButtonPressAlpha"
    )

    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.4f)
            .liquidGlass(shape = shape, tint = colors.glassTint)
            .clip(shape())
            .background(colors.textPrimary.copy(alpha = pressAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .height(44.dp)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center,
        content = content
    )
}

@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val colors = FairweatherTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressAlpha by animateFloatAsState(
        targetValue = if (pressed) 0.18f else 0f,
        animationSpec = tween(120),
        label = "glassIconButtonPressAlpha"
    )

    Box(
        modifier = modifier
            .size(size)
            .alpha(if (enabled) 1f else 0.4f)
            .liquidGlass(shape = { CircleShape }, tint = colors.glassTint)
            .clip(CircleShape)
            .background(colors.textPrimary.copy(alpha = pressAlpha))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
        content = content
    )
}