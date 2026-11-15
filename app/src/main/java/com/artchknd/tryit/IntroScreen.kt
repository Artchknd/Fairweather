package com.artchknd.tryit

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathMeasure
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.artchknd.tryit.ui.components.liquidGlass
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.tan

private data class FGeometry(
    val bounds: Rect,
    val topBarTop: Float,
    val topBarBottom: Float,
    val midBarTop: Float,
    val midBarBottom: Float,
    val midBarRight: Float,
    val stemLeft: Float,
    val stemRight: Float
)

private fun measureFGeometry(paint: Paint): FGeometry {
    val fBounds = Rect()
    paint.getTextBounds("F", 0, 1, fBounds)

    val w = (fBounds.width() + 8).coerceAtLeast(12)
    val h = (fBounds.height() + 8).coerceAtLeast(12)
    val offsetX = -fBounds.left + 4f
    val offsetY = -fBounds.top + 4f

    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val scanPaint = Paint(paint).apply {
        color = android.graphics.Color.WHITE
        alpha = 255
    }
    canvas.drawText("F", offsetX, offsetY, scanPaint)

    val legY = (offsetY + fBounds.top + fBounds.height() * 0.82f).toInt().coerceIn(0, h - 1)
    var minStemX = w - 1
    var maxStemX = 0
    for (x in 0 until w) {
        if (android.graphics.Color.alpha(bitmap.getPixel(x, legY)) > 30) {
            if (x < minStemX) minStemX = x
            if (x > maxStemX) maxStemX = x
        }
    }
    if (minStemX >= maxStemX) {
        minStemX = (w * 0.10f).toInt()
        maxStemX = (w * 0.30f).toInt()
    }

    val rightEdgeX = (offsetX + fBounds.right).toInt().coerceIn(maxStemX, w - 1)
    val scanX = (maxStemX + (rightEdgeX - maxStemX) * 0.35f).toInt().coerceIn(0, w - 1)
    val runs = mutableListOf<IntRange>()
    var inRun = false
    var runStart = 0
    for (y in 0 until h) {
        val isInk = android.graphics.Color.alpha(bitmap.getPixel(scanX, y)) > 30
        if (isInk && !inRun) {
            inRun = true
            runStart = y
        } else if (!isInk && inRun) {
            inRun = false
            runs.add(runStart until y)
        }
    }
    if (inRun) runs.add(runStart until h)

    val topBarTop: Float
    val topBarBottom: Float
    val midBarTop: Float
    val midBarBottom: Float
    val midBarRight: Float

    if (runs.size >= 2) {
        val topRun = runs[0]
        val midRun = runs[1]
        topBarTop = (topRun.first - offsetY) - 1.5f
        topBarBottom = (topRun.last - offsetY) + 0.5f
        midBarTop = (midRun.first - offsetY) - 0.5f
        midBarBottom = (midRun.last - offsetY) + 0.5f

        val midCenterY = ((midRun.first + midRun.last) / 2).coerceIn(0, h - 1)
        var maxMidX = maxStemX
        for (x in maxStemX until w) {
            if (android.graphics.Color.alpha(bitmap.getPixel(x, midCenterY)) > 30) {
                maxMidX = x
            }
        }
        midBarRight = (maxMidX - offsetX) + 2f
    } else {
        val fh = fBounds.height().toFloat()
        topBarTop = fBounds.top.toFloat() - 2f
        topBarBottom = fBounds.top + fh * 0.16f
        midBarTop = fBounds.top + fh * 0.42f
        midBarBottom = fBounds.top + fh * 0.58f
        midBarRight = fBounds.right.toFloat() + 2f
    }

    bitmap.recycle()

    return FGeometry(
        bounds = fBounds,
        topBarTop = topBarTop,
        topBarBottom = topBarBottom,
        midBarTop = midBarTop,
        midBarBottom = midBarBottom,
        midBarRight = midBarRight,
        stemLeft = (minStemX - offsetX) - 1f,
        stemRight = (maxStemX - offsetX) + 1f
    )
}

@Composable
fun FairweatherLoadingAnimation(
    isDataReady: Boolean,
    onDrawFinished: () -> Unit
) {
    val currentOnDrawFinished by rememberUpdatedState(onDrawFinished)
    val context = LocalContext.current
    val density = LocalDensity.current

    val initializingText = stringResource(R.string.intro_status_initializing)
    val preloadingText = stringResource(R.string.intro_status_preloading)

    // === 可微調的參數區 ===
    val targetText = "Fairweather"
    val desiredFontSizeSp = 56.sp
    val iconHeightDp = 164.dp            // SVG 線條實際高度（無背景框，直接依線條尺寸放大）
    val iconBottomGapDp = 30.dp          // 圖示底部與單詞頂部的間距
    val wordVerticalBias = 0.50f         // 單詞位於螢幕垂直位置比例（圖示會落在更上方約 20%~42% 處）
    val initialFScale = 1.38f            // 單詞 F 的起始放大倍率（避免與上方圖示重疊）
    val fTopBarFinishThreshold = 0.885f  // 第一橫槓畫到 98.5% 視覺已完整，避免過阻尼最後 1% 停頓感
    val pauseAfterFMs = 120L             // F 畫完後短暫停頓再讓其他字母進場
    val charStartDelayMs = 25L           // F 開始左移後多久開始畫其他字母
    val charStaggerMs = 32L
    val tiltAngleDeg = 25.0
    val letterSpacingEm = 0.015f

    // SVG 圖示繪製速度（比下方文字更快完成）
    val svgStrokeDamping = 0.95f
    val svgStrokeStiffness = 320f

    // 下方文字 F 繪製速度
    val fStrokeDamping = 1.18f
    val fStrokeStiffness = 175f
    val fStemDamping = 0.98f             // 獨立加速左豎槓（收尾更俐落）
    val fStemStiffness = 320f            // 數值越大豎槓畫越快（可調到 360f~420f）
    // ======================

    val customTypeface = remember(context) {
        // ResourcesCompat.getFont(context, R.font.noto_sans_tc) ?: Typeface.DEFAULT_BOLD
        Typeface.create("sans-serif", Typeface.BOLD)
    }
    val initFontFamily = remember(customTypeface) { FontFamily(customTypeface) }

    // 預先建立 SVG 512x512 座標空間的幾何 Path 與 PathMeasure
    val iconWindCrestPath = remember {
        Path().apply {
            moveTo(154.90f, 242.85f)
            lineTo(163.18f, 187.46f)
            arcTo(RectF(250.212f - 88f, 200.473f - 88f, 250.212f + 88f, 200.473f + 88f), -171.496f, 75.107f, false)
            arcTo(RectF(294.544f - 486f, 595.997f - 486f, 294.544f + 486f, 595.997f + 486f), -96.394f, 7.735f, false)
        }
    }
    val iconOrbitTopPath = remember {
        Path().apply {
            arcTo(RectF(239.497f - 104f, 269.994f - 104f, 239.497f + 104f, 269.994f + 104f), -72.996f, 19.429f, true)
        }
    }
    val iconOrbitRightPath = remember {
        Path().apply {
            arcTo(RectF(239.499f - 104f, 269.999f - 104f, 239.499f + 104f, 269.999f + 104f), -30.433f, 68.434f, true)
        }
    }
    val iconSpinePath = remember {
        Path().apply {
            moveTo(159.50f, 388.00f)
            lineTo(188.90f, 191.31f)
            arcTo(RectF(250.219f - 62f, 200.475f - 62f, 250.219f + 62f, 200.475f + 62f), -171.499f, 75.111f, false)
            arcTo(RectF(294.514f - 460f, 596.002f - 460f, 294.514f + 460f, 596.002f + 460f), -96.390f, 13.757f, false)
        }
    }
    val iconHorizonPath = remember {
        Path().apply {
            moveTo(215.50f, 270.00f)
            lineTo(303.50f, 270.00f)
        }
    }

    val windCrestMeasure = remember(iconWindCrestPath) { PathMeasure(iconWindCrestPath, false) }
    val orbitTopMeasure = remember(iconOrbitTopPath) { PathMeasure(iconOrbitTopPath, false) }
    val orbitRightMeasure = remember(iconOrbitRightPath) { PathMeasure(iconOrbitRightPath, false) }
    val spineMeasure = remember(iconSpinePath) { PathMeasure(iconSpinePath, false) }
    val horizonMeasure = remember(iconHorizonPath) { PathMeasure(iconHorizonPath, false) }

    val segmentPath = remember { Path() }
    val iconStrokePaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
    }
    val iconFillPaint = remember {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val maxWidthPx = with(density) { maxWidth.toPx() }
        val desiredTextSizePx = with(density) { desiredFontSizeSp.toPx() }

        val fittedTextSizePx = remember(customTypeface, desiredTextSizePx, letterSpacingEm, maxWidthPx) {
            val probePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = desiredTextSizePx
                typeface = customTypeface
                letterSpacing = letterSpacingEm
            }
            val bounds = Rect()
            probePaint.getTextBounds(targetText, 0, targetText.length, bounds)
            val maxAllowedWidth = maxWidthPx * 0.86f
            if (bounds.width() > maxAllowedWidth && bounds.width() > 0) {
                desiredTextSizePx * (maxAllowedWidth / bounds.width().toFloat())
            } else {
                desiredTextSizePx
            }
        }

        val textPaint = remember(customTypeface, fittedTextSizePx, letterSpacingEm) {
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
                textSize = fittedTextSizePx
                typeface = customTypeface
                letterSpacing = letterSpacingEm
            }
        }

        val fGeometry = remember(textPaint) { measureFGeometry(textPaint) }

        // 計算單詞左豎槓往下畫到「第二橫槓中心高度」時對應的進度 (0f..1f)
        val stemMidTriggerThreshold = remember(fGeometry) {
            val stemStart = fGeometry.topBarBottom
            val stemEnd = fGeometry.bounds.bottom.toFloat() + 2f
            val midBarCenterY = (fGeometry.midBarTop + fGeometry.midBarBottom) / 2f
            ((midBarCenterY - stemStart) / (stemEnd - stemStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }

        // 上方 SVG 圖示動畫進度
        val iconSpineProgress = remember { Animatable(0f) }
        val iconWindCrestProgress = remember { Animatable(0f) }
        val iconHorizonProgress = remember { Animatable(0f) }
        val iconOrbitTopProgress = remember { Animatable(0f) }
        val iconSunProgress = remember { Animatable(0f) }
        val iconOrbitRightProgress = remember { Animatable(0f) }

        // 下方 Fairweather 單詞動畫進度
        val fTopBarProgress = remember { Animatable(0f) }
        val fMidBarProgress = remember { Animatable(0f) }
        val fStemProgress = remember { Animatable(0f) }
        val fTransformProgress = remember { Animatable(0f) }
        val initProgress = remember { Animatable(0f) }

        val statusLabel = if (isDataReady) preloadingText else initializingText

        val nonSpaceIndices = remember {
            targetText.indices.filter { it > 0 && targetText[it] != ' ' }
        }
        val charProgresses = remember {
            nonSpaceIndices.map { Animatable(0f) }
        }

        LaunchedEffect(Unit) {
            // === 同時繪製：上方幾何線條圖示 + 下方 Fairweather 單詞 ===
            val iconJob = launch {
                val spineJob = launch {
                    iconSpineProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = svgStrokeDamping,
                            stiffness = svgStrokeStiffness,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                // 外圍高空風切弧線緊接跟上
                snapshotFlow { iconSpineProgress.value }.first { it >= 0.08f }
                val windJob = launch {
                    iconWindCrestProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = svgStrokeDamping,
                            stiffness = 340f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                // 蒼穹弧脊剛好經過 y=270 (進度 0.305) 時，繪製留白地平線
                snapshotFlow { iconSpineProgress.value }.first { it >= 0.305f }
                val horizonJob = launch {
                    iconHorizonProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = 380f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                // 弧頂轉向右側時，繪製上方日出軌道
                snapshotFlow { iconSpineProgress.value }.first { it >= 0.58f }
                val orbitTopJob = launch {
                    iconOrbitTopProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = svgStrokeDamping,
                            stiffness = 380f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                // 上段軌道接近暖陽位置時，點亮暖陽並延續下段黎明前軌跡
                snapshotFlow { iconOrbitTopProgress.value }.first { it >= 0.65f }
                val sunJob = launch {
                    iconSunProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.65f,
                            stiffness = 420f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }
                val orbitRightJob = launch {
                    iconOrbitRightProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = svgStrokeDamping,
                            stiffness = 340f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                listOf(spineJob, windJob, horizonJob, orbitTopJob, sunJob, orbitRightJob).joinAll()
            }

            val wordJob = launch {
                // 1. 先畫第一橫槓（上橫槓）
                val topBarJob = launch {
                    fTopBarProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = fStrokeDamping,
                            stiffness = fStrokeStiffness,
                            visibilityThreshold = 0.003f
                        )
                    )
                }
                snapshotFlow { fTopBarProgress.value }.first { it >= fTopBarFinishThreshold }

                // 2. 第一橫槓畫完後，接著畫左豎槓（獨立加速）
                val stemJob = launch {
                    fStemProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = fStemDamping,
                            stiffness = fStemStiffness,
                            visibilityThreshold = 0.003f
                        )
                    )
                }

                // 3. 豎槓往下畫到剛好第二橫槓的位置時，開始畫第二橫槓
                snapshotFlow { fStemProgress.value }.first { it >= stemMidTriggerThreshold }
                val midBarJob = launch {
                    fMidBarProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = fStrokeDamping,
                            stiffness = fStrokeStiffness,
                            visibilityThreshold = 0.003f
                        )
                    )
                }

                topBarJob.join()
                stemJob.join()
                midBarJob.join()

                // 4. 短暫停頓
                delay(pauseAfterFMs)

                // 5. F 縮小左移 + 其他字母進場
                val transformJob = launch {
                    fTransformProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.92f,
                            stiffness = 230f,
                            visibilityThreshold = 0.002f
                        )
                    )
                }

                delay(charStartDelayMs)
                val charJobs = charProgresses.mapIndexed { order, anim ->
                    launch {
                        delay(order * charStaggerMs)
                        anim.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.76f,
                                stiffness = 290f,
                                visibilityThreshold = 0.002f
                            )
                        )
                    }
                }

                transformJob.join()
                charJobs.joinAll()
            }

            iconJob.join()
            wordJob.join()

            // 線條動畫畫完：通知外層開始在底下預先組合主畫面，何時結束由外層決定
            currentOnDrawFinished()

            // === 底部狀態列 ===
            delay(150L)
            initProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.92f,
                    stiffness = 220f,
                    visibilityThreshold = 0.002f
                )
            )
        }


        Canvas(modifier = Modifier.fillMaxSize()) {
            val fullBounds = Rect()
            textPaint.getTextBounds(targetText, 0, targetText.length, fullBounds)

            // 單詞置於畫面中央，上方圖示依「實際線條包圍盒 (148..365, 107.75..399.50)」對齊並往上位移
            val baselineY = size.height * wordVerticalBias - fullBounds.exactCenterY()
            val wordTopY = baselineY + fullBounds.top
            val startX = size.width / 2f - fullBounds.exactCenterX()

            val svgArtCenterX = 256.5f
            val svgArtTop = 107.75f
            val svgArtHeight = 291.75f
            val iconHeightPx = iconHeightDp.toPx().coerceAtMost(size.height * 0.25f)
            val svgScale = iconHeightPx / svgArtHeight
            val iconGapPx = iconBottomGapDp.toPx()
            val iconTop = (wordTopY - iconGapPx - iconHeightPx).coerceAtLeast(size.height * 0.08f)

            val charWidths = FloatArray(targetText.length)
            textPaint.getTextWidths(targetText, charWidths)
            val charOffsets = FloatArray(targetText.length)
            var accX = 0f
            for (i in targetText.indices) {
                charOffsets[i] = accX
                accX += charWidths[i]
            }

            val fBounds = fGeometry.bounds
            val fFinalCenterX = startX + fBounds.exactCenterX()
            val fFinalCenterY = baselineY + fBounds.exactCenterY()
            val wordCenterX = size.width / 2f

            val moveP = fTransformProgress.value
            val currentFCenterX = lerp(wordCenterX, fFinalCenterX, moveP)
            val currentFCenterY = fFinalCenterY
            val currentFScale = lerp(initialFScale, 1f, moveP)

            drawIntoCanvas { canvas ->
                val native = canvas.nativeCanvas

                // === A. 繪製上方 Fairweather SVG 線條與暖陽（無背景框） ===
                native.save()
                native.translate(size.width / 2f, iconTop)
                native.scale(svgScale, svgScale)
                native.translate(-svgArtCenterX, -svgArtTop)

                // 1. 高空風切等壓弧線 (#F2EFE9, strokeWidth=4.5, opacity=0.28)
                if (iconWindCrestProgress.value > 0f) {
                    segmentPath.reset()
                    windCrestMeasure.getSegment(
                        0f,
                        windCrestMeasure.length * iconWindCrestProgress.value.coerceIn(0f, 1f),
                        segmentPath,
                        true
                    )
                    iconStrokePaint.color = 0xFFF2EFE9.toInt()
                    iconStrokePaint.strokeWidth = 4.5f
                    iconStrokePaint.alpha = (0.28f * 255).toInt()
                    native.drawPath(segmentPath, iconStrokePaint)
                }

                // 2a. 日出天體軌道上段 (#E59F52, strokeWidth=4.5, opacity=0.48)
                if (iconOrbitTopProgress.value > 0f) {
                    segmentPath.reset()
                    orbitTopMeasure.getSegment(
                        0f,
                        orbitTopMeasure.length * iconOrbitTopProgress.value.coerceIn(0f, 1f),
                        segmentPath,
                        true
                    )
                    iconStrokePaint.color = 0xFFE59F52.toInt()
                    iconStrokePaint.strokeWidth = 4.5f
                    iconStrokePaint.alpha = (0.48f * 255).toInt()
                    native.drawPath(segmentPath, iconStrokePaint)
                }

                // 2b. 日出天體軌道下段 (#E59F52, strokeWidth=4.5, opacity=0.48)
                if (iconOrbitRightProgress.value > 0f) {
                    segmentPath.reset()
                    orbitRightMeasure.getSegment(
                        0f,
                        orbitRightMeasure.length * iconOrbitRightProgress.value.coerceIn(0f, 1f),
                        segmentPath,
                        true
                    )
                    iconStrokePaint.color = 0xFFE59F52.toInt()
                    iconStrokePaint.strokeWidth = 4.5f
                    iconStrokePaint.alpha = (0.48f * 255).toInt()
                    native.drawPath(segmentPath, iconStrokePaint)
                }

                // 3. 字母「F」主體一：蒼穹弧頂與風之斜脊 (#F2EFE9, strokeWidth=23)
                if (iconSpineProgress.value > 0f) {
                    segmentPath.reset()
                    spineMeasure.getSegment(
                        0f,
                        spineMeasure.length * iconSpineProgress.value.coerceIn(0f, 1f),
                        segmentPath,
                        true
                    )
                    iconStrokePaint.color = 0xFFF2EFE9.toInt()
                    iconStrokePaint.strokeWidth = 23f
                    iconStrokePaint.alpha = 255
                    native.drawPath(segmentPath, iconStrokePaint)
                }

                // 4. 字母「F」主體二：留白地平線 (#F2EFE9, strokeWidth=23)
                if (iconHorizonProgress.value > 0f) {
                    segmentPath.reset()
                    horizonMeasure.getSegment(
                        0f,
                        horizonMeasure.length * iconHorizonProgress.value.coerceIn(0f, 1f),
                        segmentPath,
                        true
                    )
                    iconStrokePaint.color = 0xFFF2EFE9.toInt()
                    iconStrokePaint.strokeWidth = 23f
                    iconStrokePaint.alpha = 255
                    native.drawPath(segmentPath, iconStrokePaint)
                }

                // 5. 晨曦暖陽 (#E59F52, cx=316.79, cy=200.41, r=12)
                if (iconSunProgress.value > 0f) {
                    val sunP = iconSunProgress.value
                    iconFillPaint.color = 0xFFE59F52.toInt()
                    iconFillPaint.alpha = (sunP.coerceIn(0f, 1f) * 255).toInt()
                    native.drawCircle(316.79f, 200.41f, 12f * sunP.coerceAtLeast(0f), iconFillPaint)
                }

                native.restore()

                // === B. 繪製下方 Fairweather 單詞 ===
                native.save()
                native.translate(currentFCenterX, currentFCenterY)
                native.scale(currentFScale, currentFScale)
                native.translate(-fBounds.exactCenterX(), -fBounds.exactCenterY())

                val fLeft = fBounds.left.toFloat() - 2f
                val fRight = fBounds.right.toFloat() + 2f
                val fBottom = fBounds.bottom.toFloat() + 2f

                // 第一橫槓（上橫槓）
                if (fTopBarProgress.value > 0f) {
                    native.save()
                    val currentTopBarRight = lerp(fLeft, fRight, fTopBarProgress.value)
                    native.clipRect(
                        fLeft,
                        fGeometry.topBarTop,
                        currentTopBarRight,
                        fGeometry.topBarBottom
                    )
                    native.drawText("F", 0f, 0f, textPaint)
                    native.restore()
                }

                // 左豎槓
                if (fStemProgress.value > 0f) {
                    native.save()
                    val currentStemBottom = lerp(fGeometry.topBarBottom, fBottom, fStemProgress.value)
                    native.clipRect(
                        fGeometry.stemLeft,
                        fGeometry.topBarBottom - 1f,
                        fGeometry.stemRight,
                        currentStemBottom
                    )
                    native.drawText("F", 0f, 0f, textPaint)
                    native.restore()
                }

                // 第二橫槓（從豎槓右緣長出）
                if (fMidBarProgress.value > 0f) {
                    native.save()
                    val midBarStartX = (fGeometry.stemRight - 2f).coerceIn(fLeft, fGeometry.midBarRight)
                    val currentMidBarRight = lerp(midBarStartX, fGeometry.midBarRight, fMidBarProgress.value)
                    native.clipRect(
                        fLeft,
                        fGeometry.midBarTop,
                        currentMidBarRight,
                        fGeometry.midBarBottom
                    )
                    native.drawText("F", 0f, 0f, textPaint)
                    native.restore()
                }

                native.restore()

                val angleRad = Math.toRadians(tiltAngleDeg)
                val riseDistance = fittedTextSizePx * 0.85f
                val fm = textPaint.fontMetrics

                nonSpaceIndices.forEachIndexed { order, charIndex ->
                    val p = charProgresses[order].value
                    if (p <= 0.001f) return@forEachIndexed

                    val ch = targetText[charIndex].toString()
                    val charX = startX + charOffsets[charIndex]

                    val remaining = 1f - p
                    val dy = remaining * riseDistance
                    val dx = -dy * tan(angleRad).toFloat()

                    val alpha = ((p / 0.35f).coerceIn(0f, 1f) * 255).toInt()
                    textPaint.alpha = alpha

                    val revealDescenderP = ((p - 0.75f) / 0.25f).coerceIn(0f, 1f)
                    val clipBottom = lerp(baselineY, baselineY + fm.descent + 16f, revealDescenderP)

                    native.save()
                    native.clipRect(0f, 0f, size.width, clipBottom)
                    native.drawText(ch, charX + dx, baselineY + dy, textPaint)
                    native.restore()
                }
                textPaint.alpha = 255
            }
        }

        if (initProgress.value > 0f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 64.dp)
                    .graphicsLayer {
                        alpha = initProgress.value.coerceIn(0f, 1f)
                        translationY = (1f - initProgress.value) * 14.dp.toPx()
                    }
            ) {
                AnimatedContent(
                    targetState = statusLabel,
                    transitionSpec = {
                        (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 3 })
                            .togetherWith(
                                fadeOut(tween(180)) + slideOutVertically(tween(180)) { -it / 3 }
                            )
                    },
                    label = "introStatusText"
                ) { label ->
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        letterSpacing = 2.sp,
                        fontFamily = initFontFamily
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.18f),
                    strokeWidth = 2.5.dp
                )
            }
        }
    }
}

private fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}
