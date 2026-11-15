package com.artchknd.tryit.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import com.artchknd.tryit.ui.components.WeatherIconType
import com.artchknd.tryit.ui.components.weatherIconFor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object WidgetIconRenderer {

    fun render(
        condition: String,
        color: Int,
        sizePx: Int,
        conditionCode: Int? = null,
        wmoCode: Int? = null,
        isDay: Boolean = true
    ): Bitmap {
        val safeSize = sizePx.coerceAtLeast(24)
        val bitmap = Bitmap.createBitmap(safeSize, safeSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val s = safeSize.toFloat()
        val stroke = s * 0.08f
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.STROKE
            strokeWidth = stroke
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }

        when (weatherIconFor(condition, conditionCode, wmoCode, isDay)) {
            WeatherIconType.SUNNY -> drawSun(canvas, s, paint)
            WeatherIconType.CLEAR_NIGHT -> drawClearNight(canvas, s, stroke, paint)
            WeatherIconType.PARTLY_CLOUDY -> drawPartlyCloudy(canvas, s, stroke, paint)
            WeatherIconType.PARTLY_CLOUDY_NIGHT -> drawPartlyCloudyNight(canvas, s, stroke, paint)
            WeatherIconType.CLOUDY -> drawCloud(canvas, s * 0.5f, s * 0.54f, s * 0.9f, s * 0.9f, paint)
            WeatherIconType.DRIZZLE -> {
                drawCloud(canvas, s * 0.5f, s * 0.38f, s * 0.78f, s * 0.78f, paint)
                drawDrizzle(canvas, s, stroke, paint)
            }
            WeatherIconType.RAINY -> {
                drawCloud(canvas, s * 0.5f, s * 0.38f, s * 0.78f, s * 0.78f, paint)
                drawRainDrops(canvas, s, paint)
            }
            WeatherIconType.THUNDERSTORM -> {
                drawCloud(canvas, s * 0.5f, s * 0.36f, s * 0.78f, s * 0.78f, paint)
                drawLightning(canvas, s, paint)
            }
            WeatherIconType.SNOWY -> {
                drawCloud(canvas, s * 0.5f, s * 0.38f, s * 0.78f, s * 0.78f, paint)
                drawSnowflakes(canvas, s, stroke, paint)
            }
            WeatherIconType.FOGGY -> drawFog(canvas, s, paint)
        }
        return bitmap
    }

    private fun drawSun(canvas: Canvas, s: Float, paint: Paint) {
        val cx = s * 0.5f
        val cy = s * 0.5f
        canvas.drawCircle(cx, cy, s * 0.22f, paint)
        val inner = s * 0.32f
        val outer = s * 0.43f
        for (i in 0 until 8) {
            val angle = i * (PI / 4.0)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(cx + dx * inner, cy + dy * inner, cx + dx * outer, cy + dy * outer, paint)
        }
    }

    private fun drawClearNight(canvas: Canvas, s: Float, stroke: Float, paint: Paint) {
        val cx = s * 0.46f
        val cy = s * 0.52f
        val r = s * 0.28f
        val path = Path().apply {
            moveTo(cx + r * 0.22f, cy - r * 0.96f)
            cubicTo(cx - r * 0.85f, cy - r * 0.88f, cx - r * 0.95f, cy + r * 0.75f, cx + r * 0.25f, cy + r * 0.95f)
            cubicTo(cx - r * 0.25f, cy + r * 0.52f, cx - r * 0.25f, cy - r * 0.48f, cx + r * 0.22f, cy - r * 0.96f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.strokeWidth = stroke * 0.85f
        drawStarCross(canvas, s * 0.74f, s * 0.28f, s * 0.08f, paint)
        drawStarCross(canvas, s * 0.80f, s * 0.54f, s * 0.055f, paint)
        paint.strokeWidth = stroke
    }

    private fun drawStarCross(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint) {
        canvas.drawLine(cx - r, cy, cx + r, cy, paint)
        canvas.drawLine(cx, cy - r, cx, cy + r, paint)
    }

    private fun drawPartlyCloudy(canvas: Canvas, s: Float, stroke: Float, paint: Paint) {
        val sunX = s * 0.68f
        val sunY = s * 0.32f
        canvas.drawCircle(sunX, sunY, s * 0.16f, paint)
        paint.strokeWidth = stroke * 0.9f
        val inner = s * 0.23f
        val outer = s * 0.30f
        for (i in listOf(-1, 0, 1, 6, 7)) {
            val angle = i * (PI / 4.0)
            val dx = cos(angle).toFloat()
            val dy = sin(angle).toFloat()
            canvas.drawLine(sunX + dx * inner, sunY + dy * inner, sunX + dx * outer, sunY + dy * outer, paint)
        }
        paint.strokeWidth = stroke
        drawCloud(canvas, s * 0.44f, s * 0.62f, s * 0.78f, s * 0.78f, paint)
    }

    private fun drawPartlyCloudyNight(canvas: Canvas, s: Float, stroke: Float, paint: Paint) {
        val cx = s * 0.68f
        val cy = s * 0.32f
        val r = s * 0.18f
        val moonPath = Path().apply {
            moveTo(cx + r * 0.15f, cy - r * 0.92f)
            cubicTo(cx - r * 0.78f, cy - r * 0.80f, cx - r * 0.85f, cy + r * 0.62f, cx + r * 0.20f, cy + r * 0.88f)
            cubicTo(cx - r * 0.22f, cy + r * 0.45f, cx - r * 0.22f, cy - r * 0.45f, cx + r * 0.15f, cy - r * 0.92f)
            close()
        }
        paint.strokeWidth = stroke * 0.9f
        canvas.drawPath(moonPath, paint)
        paint.strokeWidth = stroke
        drawCloud(canvas, s * 0.44f, s * 0.62f, s * 0.78f, s * 0.78f, paint)
    }

    private fun drawCloud(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float, paint: Paint) {
        val base = cy + h * 0.22f
        val leftX = cx - w * 0.30f
        val rightX = cx + w * 0.30f
        val path = Path().apply {
            moveTo(leftX, base)
            lineTo(rightX, base)
            cubicTo(cx + w * 0.46f, base, cx + w * 0.46f, base - h * 0.28f, cx + w * 0.24f, base - h * 0.28f)
            cubicTo(cx + w * 0.20f, base - h * 0.54f, cx - w * 0.14f, base - h * 0.56f, cx - w * 0.20f, base - h * 0.30f)
            cubicTo(cx - w * 0.44f, base - h * 0.30f, cx - w * 0.46f, base, leftX, base)
            close()
        }
        canvas.drawPath(path, paint)
    }

    private fun drawDrizzle(canvas: Canvas, s: Float, stroke: Float, paint: Paint) {
        paint.strokeWidth = stroke * 0.9f
        val topY = s * 0.67f
        val midY = s * 0.78f
        listOf(0.34f, 0.50f, 0.66f).forEachIndexed { idx, xRatio ->
            val x = s * xRatio
            val offsetY = if (idx == 1) s * 0.06f else 0f
            canvas.drawLine(x, topY + offsetY, x - s * 0.03f, midY + offsetY, paint)
        }
        paint.strokeWidth = stroke
    }

    private fun drawRainDrops(canvas: Canvas, s: Float, paint: Paint) {
        val topY = s * 0.66f
        val botY = s * 0.86f
        for (xRatio in listOf(0.34f, 0.50f, 0.66f)) {
            val x = s * xRatio
            canvas.drawLine(x, topY, x - s * 0.06f, botY, paint)
        }
    }

    private fun drawLightning(canvas: Canvas, s: Float, paint: Paint) {
        val path = Path().apply {
            moveTo(s * 0.53f, s * 0.58f)
            lineTo(s * 0.43f, s * 0.74f)
            lineTo(s * 0.53f, s * 0.74f)
            lineTo(s * 0.45f, s * 0.90f)
        }
        canvas.drawPath(path, paint)
    }

    private fun drawSnowflakes(canvas: Canvas, s: Float, stroke: Float, paint: Paint) {
        paint.strokeWidth = stroke * 0.85f
        val r = s * 0.045f
        for ((xr, yr) in listOf(0.34f to 0.74f, 0.50f to 0.82f, 0.66f to 0.74f)) {
            canvas.drawCircle(s * xr, s * yr, r, paint)
        }
        paint.strokeWidth = stroke
    }

    private fun drawFog(canvas: Canvas, s: Float, paint: Paint) {
        val rows = listOf(
            Triple(0.20f, 0.80f, 0.34f),
            Triple(0.14f, 0.72f, 0.50f),
            Triple(0.26f, 0.86f, 0.66f)
        )
        for ((startX, endX, y) in rows) {
            canvas.drawLine(s * startX, s * y, s * endX, s * y, paint)
        }
    }
}