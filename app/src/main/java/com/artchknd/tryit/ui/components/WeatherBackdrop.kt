package com.artchknd.tryit.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun WeatherBackdrop(
    condition: String,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    conditionCode: Int? = null,
    wmoCode: Int? = null,
    isDay: Boolean = true
) {
    val kind = weatherIconFor(
        condition = condition,
        conditionCode = conditionCode,
        wmoCode = wmoCode,
        isDay = isDay
    )
    val transition = rememberInfiniteTransition(label = "weatherBackdrop")

    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloudDrift"
    )

    val precipCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (kind == WeatherIconType.SNOWY) 3600 else 1100,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "precipCycle"
    )

    val useNightPalette = isDark || !isDay
    val skyColors = backdropColors(kind, useNightPalette)
    val atmosphereBlur = if (kind == WeatherIconType.FOGGY) 26.dp else 18.dp

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(skyColors))
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(atmosphereBlur)
        ) {
            when (kind) {
                WeatherIconType.SUNNY -> drawSunnyAtmosphere(useNightPalette, drift)
                WeatherIconType.CLEAR_NIGHT -> drawNightAtmosphere(drift)
                WeatherIconType.PARTLY_CLOUDY -> {
                    drawSunnyAtmosphere(useNightPalette, drift)
                    drawDriftingClouds(useNightPalette, drift, densityFactor = 0.75f)
                }
                WeatherIconType.PARTLY_CLOUDY_NIGHT -> {
                    drawNightAtmosphere(drift)
                    drawDriftingClouds(useNightPalette, drift, densityFactor = 0.75f)
                }
                WeatherIconType.CLOUDY -> drawDriftingClouds(useNightPalette, drift, densityFactor = 1.1f)
                WeatherIconType.DRIZZLE,
                WeatherIconType.RAINY,
                WeatherIconType.THUNDERSTORM -> {
                    drawDriftingClouds(useNightPalette, drift, densityFactor = 1.0f, stormy = true)
                }
                WeatherIconType.SNOWY -> drawDriftingClouds(useNightPalette, drift, densityFactor = 0.9f)
                WeatherIconType.FOGGY -> drawFogBands(useNightPalette, drift)
            }
        }

        if (kind == WeatherIconType.CLEAR_NIGHT || kind == WeatherIconType.PARTLY_CLOUDY_NIGHT) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawNightStars()
            }
        }

        if (kind == WeatherIconType.DRIZZLE || kind == WeatherIconType.RAINY || kind == WeatherIconType.THUNDERSTORM) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRainStreaks(
                    isDark = useNightPalette,
                    cycle = precipCycle,
                    lightDrizzle = kind == WeatherIconType.DRIZZLE
                )
            }
        }

        if (kind == WeatherIconType.SNOWY) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawSnowParticles(useNightPalette, precipCycle)
            }
        }
    }
}

private fun backdropColors(kind: WeatherIconType, isDark: Boolean): List<Color> {
    if (isDark) {
        return when (kind) {
            WeatherIconType.SUNNY, WeatherIconType.CLEAR_NIGHT -> listOf(
                Color(0xFF070F26), Color(0xFF132652), Color(0xFF1F3B73)
            )
            WeatherIconType.PARTLY_CLOUDY, WeatherIconType.PARTLY_CLOUDY_NIGHT -> listOf(
                Color(0xFF0C1830), Color(0xFF1C2D4A), Color(0xFF2B4162)
            )
            WeatherIconType.CLOUDY -> listOf(
                Color(0xFF141E2E), Color(0xFF233044), Color(0xFF324158)
            )
            WeatherIconType.DRIZZLE,
            WeatherIconType.RAINY,
            WeatherIconType.THUNDERSTORM -> listOf(
                Color(0xFF09111F), Color(0xFF162338), Color(0xFF21314A)
            )
            WeatherIconType.SNOWY -> listOf(
                Color(0xFF142033), Color(0xFF22334D), Color(0xFF334966)
            )
            WeatherIconType.FOGGY -> listOf(
                Color(0xFF19222D), Color(0xFF293544), Color(0xFF3B495B)
            )
        }
    }
    return when (kind) {
        WeatherIconType.SUNNY -> listOf(
            Color(0xFF3B95F2), Color(0xFF6EC0FC), Color(0xFFAEE2FF)
        )
        WeatherIconType.CLEAR_NIGHT, WeatherIconType.PARTLY_CLOUDY_NIGHT -> listOf(
            Color(0xFF0B1530), Color(0xFF182A52), Color(0xFF294172)
        )
        WeatherIconType.PARTLY_CLOUDY -> listOf(
            Color(0xFF5195D9), Color(0xFF83B8E8), Color(0xFFBBD9F5)
        )
        WeatherIconType.CLOUDY -> listOf(
            Color(0xFF6B849E), Color(0xFF93AABF), Color(0xFFBDCAD8)
        )
        WeatherIconType.DRIZZLE,
        WeatherIconType.RAINY,
        WeatherIconType.THUNDERSTORM -> listOf(
            Color(0xFF475C72), Color(0xFF677E94), Color(0xFF8A9FB2)
        )
        WeatherIconType.SNOWY -> listOf(
            Color(0xFF6C8AA8), Color(0xFF9BB4CC), Color(0xFFD2E0EC)
        )
        WeatherIconType.FOGGY -> listOf(
            Color(0xFFB3C2D1), Color(0xFFD0DBE6), Color(0xFFE8EEF4)
        )
    }
}

private fun DrawScope.drawSunnyAtmosphere(isDark: Boolean, drift: Float) {
    val sunCenter = Offset(
        x = size.width * (0.82f - drift * 0.04f),
        y = size.height * 0.16f
    )
    val glowRadius = size.minDimension * 0.65f
    val coreColor = if (isDark) Color(0xFF93C5FD).copy(alpha = 0.24f) else Color(0xFFFFF7D6).copy(alpha = 0.85f)
    val midColor = if (isDark) Color(0xFF60A5FA).copy(alpha = 0.08f) else Color(0xFFFFE59E).copy(alpha = 0.30f)

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(coreColor, midColor, Color.Transparent),
            center = sunCenter,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = sunCenter
    )
}

private fun DrawScope.drawNightAtmosphere(drift: Float) {
    val moonCenter = Offset(
        x = size.width * (0.80f - drift * 0.03f),
        y = size.height * 0.16f
    )
    val glowRadius = size.minDimension * 0.55f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFE2E8F0).copy(alpha = 0.22f),
                Color(0xFF93C5FD).copy(alpha = 0.08f),
                Color.Transparent
            ),
            center = moonCenter,
            radius = glowRadius
        ),
        radius = glowRadius,
        center = moonCenter
    )
}

private fun DrawScope.drawNightStars() {
    val starColor = Color.White.copy(alpha = 0.45f)
    val positions = listOf(
        0.14f to 0.12f, 0.28f to 0.08f, 0.46f to 0.15f,
        0.62f to 0.09f, 0.18f to 0.26f, 0.38f to 0.22f,
        0.72f to 0.27f, 0.88f to 0.10f, 0.54f to 0.32f
    )
    positions.forEachIndexed { idx, (xr, yr) ->
        val r = if (idx % 3 == 0) 2.6f else 1.8f
        drawCircle(color = starColor, radius = r, center = Offset(size.width * xr, size.height * yr))
    }
}

private fun DrawScope.drawDriftingClouds(
    isDark: Boolean,
    drift: Float,
    densityFactor: Float,
    stormy: Boolean = false
) {
    val baseAlpha = when {
        stormy && isDark -> 0.26f
        stormy -> 0.42f
        isDark -> 0.18f
        else -> 0.58f
    }
    val cloudColor = if (stormy) {
        if (isDark) Color(0xFF64748B) else Color(0xFFCFD8E3)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color.White
    }

    val clouds = listOf(
        Triple(0.18f + drift * 0.10f, 0.22f, 0.28f),
        Triple(0.76f - drift * 0.08f, 0.30f, 0.32f),
        Triple(0.32f + drift * 0.12f, 0.54f, 0.34f),
        Triple(0.74f - drift * 0.10f, 0.72f, 0.30f)
    )

    clouds.forEach { (cxRatio, cyRatio, rRatio) ->
        val cx = size.width * cxRatio
        val cy = size.height * cyRatio
        val r = size.minDimension * rRatio * densityFactor
        val c = cloudColor.copy(alpha = baseAlpha)

        drawCircle(color = c, radius = r * 0.72f, center = Offset(cx - r * 0.55f, cy + r * 0.12f))
        drawCircle(color = c, radius = r * 0.90f, center = Offset(cx, cy - r * 0.08f))
        drawCircle(color = c, radius = r * 0.68f, center = Offset(cx + r * 0.60f, cy + r * 0.14f))
    }
}

private fun DrawScope.drawFogBands(isDark: Boolean, drift: Float) {
    val fogColor = if (isDark) Color(0xFFCBD5E1).copy(alpha = 0.14f) else Color.White.copy(alpha = 0.52f)
    val bands = listOf(0.18f, 0.36f, 0.54f, 0.74f, 0.88f)
    bands.forEachIndexed { idx, yRatio ->
        val shift = if (idx % 2 == 0) drift * 0.12f else -drift * 0.12f
        val center = Offset(size.width * (0.5f + shift), size.height * yRatio)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(fogColor, Color.Transparent),
                center = center,
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = center
        )
    }
}

private fun DrawScope.drawRainStreaks(isDark: Boolean, cycle: Float, lightDrizzle: Boolean) {
    val dropColor = if (isDark) {
        Color(0xFFBAE6FD).copy(alpha = if (lightDrizzle) 0.16f else 0.24f)
    } else {
        Color.White.copy(alpha = if (lightDrizzle) 0.28f else 0.38f)
    }
    val columns = if (lightDrizzle) 18 else 26
    val slantX = -size.width * (if (lightDrizzle) 0.025f else 0.05f)
    val streakLen = size.height * (if (lightDrizzle) 0.032f else 0.06f)

    for (i in 0 until columns) {
        val seedOffset = (i * 0.37f) % 1f
        val yProgress = (cycle + seedOffset) % 1f
        val x = size.width * ((i + 0.5f) / columns) + ((i % 3) - 1) * 12f
        val startY = -streakLen + yProgress * (size.height + streakLen * 2f)

        drawLine(
            color = dropColor,
            start = Offset(x, startY),
            end = Offset(x + slantX, startY + streakLen),
            strokeWidth = if (lightDrizzle) 1.6f else 2.2f,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawSnowParticles(isDark: Boolean, cycle: Float) {
    val snowColor = Color.White.copy(alpha = if (isDark) 0.42f else 0.65f)
    val columns = 20
    for (i in 0 until columns) {
        val seedOffset = (i * 0.43f) % 1f
        val yProgress = (cycle + seedOffset) % 1f
        val sway = kotlin.math.sin((yProgress * 2f * Math.PI + i).toDouble()).toFloat() * 14f
        val x = size.width * ((i + 0.5f) / columns) + sway
        val y = -16f + yProgress * (size.height + 32f)
        val r = if (i % 3 == 0) 4.2f else 2.8f
        drawCircle(color = snowColor, radius = r, center = Offset(x, y))
    }
}