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
    CLEAR_NIGHT,
    PARTLY_CLOUDY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    DRIZZLE,
    RAINY,
    THUNDERSTORM,
    SNOWY,
    FOGGY
}

fun weatherIconFor(
    condition: String,
    conditionCode: Int? = null,
    wmoCode: Int? = null,
    isDay: Boolean = true
): WeatherIconType {
    if (wmoCode != null) {
        return wmoIconFor(wmoCode, isDay)
    }
    if (conditionCode != null) {
        cwaIconFor(conditionCode)?.let { return it }
    }
    return keywordIconFor(condition)
}

private fun wmoIconFor(code: Int, isDay: Boolean): WeatherIconType {
    return when (code) {
        0, 1 -> if (isDay) WeatherIconType.SUNNY else WeatherIconType.CLEAR_NIGHT
        2 -> if (isDay) WeatherIconType.PARTLY_CLOUDY else WeatherIconType.PARTLY_CLOUDY_NIGHT
        3 -> WeatherIconType.CLOUDY
        45, 48 -> WeatherIconType.FOGGY
        51, 53, 55, 56, 57 -> WeatherIconType.DRIZZLE
        61, 63, 65, 66, 67, 80, 81, 82 -> WeatherIconType.RAINY
        71, 73, 75, 77, 85, 86 -> WeatherIconType.SNOWY
        95, 96, 99 -> WeatherIconType.THUNDERSTORM
        else -> WeatherIconType.CLOUDY
    }
}

private fun cwaIconFor(code: Int): WeatherIconType? {
    return when (code) {
        1 -> WeatherIconType.SUNNY
        2, 3 -> WeatherIconType.PARTLY_CLOUDY
        4, 5, 6, 7 -> WeatherIconType.CLOUDY
        8, 9, 10, 11, 12, 13, 14, 19, 20, 29, 30, 31, 32, 37, 38, 39 -> WeatherIconType.RAINY
        15, 16, 17, 18, 21, 22, 33, 34, 35, 36, 41 -> WeatherIconType.THUNDERSTORM
        23, 42 -> WeatherIconType.SNOWY
        24, 25, 26, 27, 28 -> WeatherIconType.FOGGY
        else -> null
    }
}

private fun keywordIconFor(condition: String): WeatherIconType {
    return when {
        condition.contains("雷") || condition.contains("Thunder", ignoreCase = true) ->
            WeatherIconType.THUNDERSTORM
        condition.contains("雪") || condition.contains("Snow", ignoreCase = true) ->
            WeatherIconType.SNOWY
        condition.contains("雨") || condition.contains("Rain", ignoreCase = true) ||
                condition.contains("Drizzle", ignoreCase = true) ||
                condition.contains("Shower", ignoreCase = true) -> WeatherIconType.RAINY
        condition.contains("霧") || condition.contains("霾") ||
                condition.contains("Fog", ignoreCase = true) -> WeatherIconType.FOGGY
        condition.contains("晴") && (condition.contains("雲") || condition.contains("陰")) ->
            WeatherIconType.PARTLY_CLOUDY
        condition.contains("晴") || condition.contains("Clear", ignoreCase = true) ||
                condition.contains("Sunny", ignoreCase = true) -> WeatherIconType.SUNNY
        condition.contains("雲") || condition.contains("陰") ||
                condition.contains("Cloud", ignoreCase = true) ||
                condition.contains("Overcast", ignoreCase = true) -> WeatherIconType.CLOUDY
        else -> WeatherIconType.PARTLY_CLOUDY
    }
}

@Composable
fun WeatherIcon(
    condition: String,
    tint: Color,
    modifier: Modifier = Modifier.size(28.dp),
    conditionCode: Int? = null,
    wmoCode: Int? = null,
    isDay: Boolean = true
) {
    val type = weatherIconFor(
        condition = condition,
        conditionCode = conditionCode,
        wmoCode = wmoCode,
        isDay = isDay
    )
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.08f
        when (type) {
            WeatherIconType.SUNNY -> drawSun(tint, strokeWidth)
            WeatherIconType.CLEAR_NIGHT -> drawClearNight(tint, strokeWidth)
            WeatherIconType.PARTLY_CLOUDY -> drawPartlyCloudy(tint, strokeWidth)
            WeatherIconType.PARTLY_CLOUDY_NIGHT -> drawPartlyCloudyNight(tint, strokeWidth)
            WeatherIconType.CLOUDY -> drawCloud(
                tint = tint,
                strokeWidth = strokeWidth,
                center = Offset(size.width * 0.5f, size.height * 0.54f),
                scale = 0.9f
            )
            WeatherIconType.DRIZZLE -> {
                drawCloud(
                    tint = tint,
                    strokeWidth = strokeWidth,
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                    scale = 0.78f
                )
                drawDrizzle(tint, strokeWidth)
            }
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
            WeatherIconType.SNOWY -> {
                drawCloud(
                    tint = tint,
                    strokeWidth = strokeWidth,
                    center = Offset(size.width * 0.5f, size.height * 0.38f),
                    scale = 0.78f
                )
                drawSnowflakes(tint, strokeWidth)
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

private fun DrawScope.drawClearNight(tint: Color, strokeWidth: Float) {
    val moonPath = Path().apply {
        val cx = size.width * 0.46f
        val cy = size.height * 0.52f
        val r = size.minDimension * 0.28f
        moveTo(cx + r * 0.22f, cy - r * 0.96f)
        cubicTo(
            cx - r * 0.85f, cy - r * 0.88f,
            cx - r * 0.95f, cy + r * 0.75f,
            cx + r * 0.25f, cy + r * 0.95f
        )
        cubicTo(
            cx - r * 0.25f, cy + r * 0.52f,
            cx - r * 0.25f, cy - r * 0.48f,
            cx + r * 0.22f, cy - r * 0.96f
        )
        close()
    }
    drawPath(
        path = moonPath,
        color = tint,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
    drawStarCross(Offset(size.width * 0.74f, size.height * 0.28f), size.minDimension * 0.08f, tint, strokeWidth * 0.85f)
    drawStarCross(Offset(size.width * 0.80f, size.height * 0.54f), size.minDimension * 0.055f, tint, strokeWidth * 0.8f)
}

private fun DrawScope.drawStarCross(center: Offset, radius: Float, tint: Color, strokeWidth: Float) {
    drawLine(
        color = tint,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = tint,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
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

private fun DrawScope.drawPartlyCloudyNight(tint: Color, strokeWidth: Float) {
    val cx = size.width * 0.68f
    val cy = size.height * 0.32f
    val r = size.minDimension * 0.18f
    val moonPath = Path().apply {
        moveTo(cx + r * 0.15f, cy - r * 0.92f)
        cubicTo(
            cx - r * 0.78f, cy - r * 0.80f,
            cx - r * 0.85f, cy + r * 0.62f,
            cx + r * 0.20f, cy + r * 0.88f
        )
        cubicTo(
            cx - r * 0.22f, cy + r * 0.45f,
            cx - r * 0.22f, cy - r * 0.45f,
            cx + r * 0.15f, cy - r * 0.92f
        )
        close()
    }
    drawPath(
        path = moonPath,
        color = tint,
        style = Stroke(width = strokeWidth * 0.9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
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

private fun DrawScope.drawDrizzle(tint: Color, strokeWidth: Float) {
    val topY = size.height * 0.67f
    val midY = size.height * 0.78f
    val xs = listOf(0.34f, 0.50f, 0.66f)
    xs.forEachIndexed { idx, xRatio ->
        val x = size.width * xRatio
        val offsetY = if (idx == 1) size.height * 0.06f else 0f
        drawLine(
            color = tint,
            start = Offset(x, topY + offsetY),
            end = Offset(x - size.width * 0.03f, midY + offsetY),
            strokeWidth = strokeWidth * 0.9f,
            cap = StrokeCap.Round
        )
    }
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

private fun DrawScope.drawSnowflakes(tint: Color, strokeWidth: Float) {
    val r = size.minDimension * 0.045f
    val points = listOf(
        Offset(size.width * 0.34f, size.height * 0.74f),
        Offset(size.width * 0.50f, size.height * 0.82f),
        Offset(size.width * 0.66f, size.height * 0.74f)
    )
    points.forEach { p ->
        drawCircle(color = tint, radius = r, center = p, style = Stroke(strokeWidth * 0.85f))
    }
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