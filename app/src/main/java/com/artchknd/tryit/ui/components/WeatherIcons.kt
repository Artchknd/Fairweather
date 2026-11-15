package com.artchknd.tryit.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class WeatherIconType {
    SUNNY,
    PARTLY_CLOUDY,
    CLOUDY,
    RAINY,
    THUNDERSTORM,
    FOGGY
}

fun weatherIconFor(condition: String): WeatherIconType {
    return when {
        condition.contains("雷") -> WeatherIconType.THUNDERSTORM
        condition.contains("雨") -> WeatherIconType.RAINY
        condition.contains("霧") || condition.contains("霾") -> WeatherIconType.FOGGY
        condition.contains("晴") && (condition.contains("雲") || condition.contains("陰")) ->
            WeatherIconType.PARTLY_CLOUDY
        condition.contains("晴") -> WeatherIconType.SUNNY
        condition.contains("雲") || condition.contains("陰") -> WeatherIconType.CLOUDY
        else -> WeatherIconType.PARTLY_CLOUDY
    }
}

@Composable
fun WeatherIcon(
    condition: String,
    tint: Color,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val type = weatherIconFor(condition)
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.08f
        when (type) {
            WeatherIconType.SUNNY -> drawSun(tint, strokeWidth)
            WeatherIconType.PARTLY_CLOUDY -> drawPartlyCloudy(tint, strokeWidth)
            WeatherIconType.CLOUDY -> drawCloud(
                tint = tint,
                strokeWidth = strokeWidth,
                center = Offset(size.width * 0.5f, size.height * 0.54f),
                scale = 0.9f
            )
            WeatherIconType.RAINY -> {
                drawCloud(
                    tint = tint,
                    strokeWidth = strokeWidth,
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                    scale = 0.78f
                )
                drawRainDrops(tint, strokeWidth)
            }
            WeatherIconType.THUNDERSTORM -> {
                drawCloud(
                    tint = tint,
                    strokeWidth = strokeWidth,
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    scale = 0.78f
                )
                drawLightning(tint, strokeWidth)
            }
            WeatherIconType.FOGGY -> drawFog(tint, strokeWidth)
        }
    }
}

private fun DrawScope.drawSun(tint: Color, strokeWidth: Float) {
    val c = Offset(size.width * 0.5f, size.height * 0.5f)
    val r = size.minDimension * 0.22f
    drawCircle(color = tint, radius = r, center = c, style = Stroke(strokeWidth))
    val inner = size.minDimension * 0.32f
    val outer = size.minDimension * 0.43f
    for (i in 0 until 8) {
        val angle = i * (PI / 4.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = tint,
            start = Offset(c.x + dx * inner, c.y + dy * inner),
            end = Offset(c.x + dx * outer, c.y + dy * outer),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawPartlyCloudy(tint: Color, strokeWidth: Float) {
    val sunCenter = Offset(size.width * 0.68f, size.height * 0.32f)
    val sunR = size.minDimension * 0.16f
    drawCircle(color = tint, radius = sunR, center = sunCenter, style = Stroke(strokeWidth))
    val inner = size.minDimension * 0.23f
    val outer = size.minDimension * 0.30f
    for (i in listOf(-1, 0, 1, 6, 7)) {
        val angle = i * (PI / 4.0)
        val dx = cos(angle).toFloat()
        val dy = sin(angle).toFloat()
        drawLine(
            color = tint,
            start = Offset(sunCenter.x + dx * inner, sunCenter.y + dy * inner),
            end = Offset(sunCenter.x + dx * outer, sunCenter.y + dy * outer),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
    }
    drawCloud(
        tint = tint,
        strokeWidth = strokeWidth,
        center = Offset(size.width * 0.44f, size.height * 0.62f),
        scale = 0.78f
    )
}

private fun DrawScope.drawCloud(
    tint: Color,
    strokeWidth: Float,
    center: Offset,
    scale: Float
) {
    val w = size.width * scale
    val h = size.height * scale
    val base = center.y + h * 0.22f
    val leftX = center.x - w * 0.30f
    val rightX = center.x + w * 0.30f

    val path = Path().apply {
        moveTo(leftX, base)
        lineTo(rightX, base)
        cubicTo(
            center.x + w * 0.46f, base,
            center.x + w * 0.46f, base - h * 0.28f,
            center.x + w * 0.24f, base - h * 0.28f
        )
        cubicTo(
            center.x + w * 0.20f, base - h * 0.54f,
            center.x - w * 0.14f, base - h * 0.56f,
            center.x - w * 0.20f, base - h * 0.30f
        )
        cubicTo(
            center.x - w * 0.44f, base - h * 0.30f,
            center.x - w * 0.46f, base,
            leftX, base
        )
        close()
    }
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawRainDrops(tint: Color, strokeWidth: Float) {
    val topY = size.height * 0.66f
    val botY = size.height * 0.86f
    val xs = listOf(0.34f, 0.50f, 0.66f)
    xs.forEach { xRatio ->
        val x = size.width * xRatio
        drawLine(
            color = tint,
            start = Offset(x, topY),
            end = Offset(x - size.width * 0.06f, botY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawLightning(tint: Color, strokeWidth: Float) {
    val path = Path().apply {
        moveTo(size.width * 0.53f, size.height * 0.58f)
        lineTo(size.width * 0.43f, size.height * 0.74f)
        lineTo(size.width * 0.53f, size.height * 0.74f)
        lineTo(size.width * 0.45f, size.height * 0.90f)
    }
    drawPath(
        path = path,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private fun DrawScope.drawFog(tint: Color, strokeWidth: Float) {
    val rows = listOf(
        Triple(0.20f, 0.80f, 0.34f),
        Triple(0.14f, 0.72f, 0.50f),
        Triple(0.26f, 0.86f, 0.66f)
    )
    rows.forEach { (startX, endX, y) ->
        drawLine(
            color = tint,
            start = Offset(size.width * startX, size.height * y),
            end = Offset(size.width * endX, size.height * y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}