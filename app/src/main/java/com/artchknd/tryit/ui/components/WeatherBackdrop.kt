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
    modifier: Modifier = Modifier
) {
    val kind = weatherIconFor(condition)
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

    val rainCycle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainCycle"
    )

    val skyColors = backdropColors(kind, isDark)
    val atmosphereBlur = if (kind == WeatherIconType.FOGGY) 26.dp else 18.dp

    Box(modifier = modifier.fillMaxSize()) {
        // 底層漸層天空
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(skyColors))
        )

        // 中層柔和雲朵／陽光暈／霧氣（透過 minSdk 31 原生 blur 做出自然大氣感）
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .blur(atmosphereBlur)
        ) {
            when (kind) {
                WeatherIconType.SUNNY -> drawSunnyAtmosphere(isDark, drift)
                WeatherIconType.PARTLY_CLOUDY -> {
                    drawSunnyAtmosphere(isDark, drift)
                    drawDriftingClouds(isDark, drift, densityFactor = 0.75f)
                }
                WeatherIconType.CLOUDY -> drawDriftingClouds(isDark, drift, densityFactor = 1.1f)
                WeatherIconType.RAINY, WeatherIconType.THUNDERSTORM -> {
                    drawDriftingClouds(isDark, drift, densityFactor = 1.0f, stormy = true)
                }
                WeatherIconType.FOGGY -> drawFogBands(isDark, drift)
            }
        }

        // 前層雨絲（不模糊，保持雨滴線條感與玻璃折射層次）
        if (kind == WeatherIconType.RAINY || kind == WeatherIconType.THUNDERSTORM) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRainStreaks(isDark, rainCycle)
            }
        }
    }
}

private fun backdropColors(kind: WeatherIconType, isDark: Boolean): List<Color> {
    if (isDark) {
        return when (kind) {
            WeatherIconType.SUNNY -> listOf(
                Color(0xFF070F26), Color(0xFF132652), Color(0xFF1F3B73)
            )
            WeatherIconType.PARTLY_CLOUDY -> listOf(
                Color(0xFF0C1830), Color(0xFF1C2D4A), Color(0xFF2B4162)
            )
            WeatherIconType.CLOUDY -> listOf(
                Color(0xFF141E2E), Color(0xFF233044), Color(0xFF324158)
            )
            WeatherIconType.RAINY, WeatherIconType.THUNDERSTORM -> listOf(
                Color(0xFF09111F), Color(0xFF162338), Color(0xFF21314A)
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
        WeatherIconType.PARTLY_CLOUDY -> listOf(
            Color(0xFF5195D9), Color(0xFF83B8E8), Color(0xFFBBD9F5)
        )
        WeatherIconType.CLOUDY -> listOf(
            Color(0xFF6B849E), Color(0xFF93AABF), Color(0xFFBDCAD8)
        )
        WeatherIconType.RAINY, WeatherIconType.THUNDERSTORM -> listOf(
            Color(0xFF475C72), Color(0xFF677E94), Color(0xFF8A9FB2)
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

private fun DrawScope.drawRainStreaks(isDark: Boolean, cycle: Float) {
    val dropColor = if (isDark) {
        Color(0xFFBAE6FD).copy(alpha = 0.24f)
    } else {
        Color.White.copy(alpha = 0.38f)
    }
    val columns = 26
    val slantX = -size.width * 0.05f
    val streakLen = size.height * 0.06f

    for (i in 0 until columns) {
        val seedOffset = (i * 0.37f) % 1f
        val yProgress = (cycle + seedOffset) % 1f
        val x = size.width * ((i + 0.5f) / columns) + ((i % 3) - 1) * 12f
        val startY = -streakLen + yProgress * (size.height + streakLen * 2f)

        drawLine(
            color = dropColor,
            start = Offset(x, startY),
            end = Offset(x + slantX, startY + streakLen),
            strokeWidth = 2.2f,
            cap = StrokeCap.Round
        )
    }
}