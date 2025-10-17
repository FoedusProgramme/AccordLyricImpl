package org.foedusprogramme.accordlyricimpl

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.View
import android.view.animation.PathInterpolator
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withClip

class LyricTextView(
    context: Context,
) : View(context) {

    constructor(context: Context, lyric: LyricBase) : this(context) {
        when (lyric) {
            is Lyric -> { this.lyric = lyric.content }
            is SyncedLyric -> {
                this.syncedLyric = lyric
                liftupProgress = FloatArray(this.syncedLyric!!.list.size)
                liftupStartupTimes = LongArray(this.syncedLyric!!.list.size) { -1 }
            }
            is Creator -> {
                this.isHolding = true
                this.lyric = "Written by: ${lyric.content}"
                setCreatorContent()
            }
        }
    }

    private val horizontalMargin = 12.dp.px.toInt()
    private val verticalMargin = 8.dp.px.toInt()

    private val lyricPaint = TextPaint().apply {
        textSize = NORMAL_TEXT_SIZE.sp.px
        color = Color.WHITE
        blendMode = BlendMode.OVERLAY
        typeface = resources.getFont(R.font.inter_bold)
    }

    var lyric: String = ""
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }
        get() {
            syncedLyric?.let {
                return it.list.joinToString(separator = "") { lyric -> lyric.content }
            }
            return field
        }

    var syncedLyric: SyncedLyric? = null

    var staticLayout: StaticLayout? = null
    var isHolding: Boolean = false

    override fun onDraw(canvas: Canvas) {
        canvas.withTranslation(
            horizontalMargin.toFloat(),
            verticalMargin.toFloat()
        ) {
            if (syncedLyric != null) {
                drawContentLayerWithProgress(canvas)
                drawHighlightLayer(canvas)
            } else {
                drawContentLayer(canvas)
            }
        }
    }

    private var animationUnit = -1
    private var animationFraction = 0F

    // This array records per-unit uplift progress.
    // For default duration, the uplift duration is
    // 1000 ms, and the duration totally relies on
    // the relative time we calculated in animate().
    // Maybe a unit's time is shorter than 1000 ms,
    // In that case, the liftup progress will still
    // be calculated so it can be used in drawConte
    // ntLayerWithProgress.
    private var liftupProgress: FloatArray? = null
    private var liftupStartupTimes: LongArray? = null
    private val progressSoFar: Long
        get() =
            (syncedLyric?.relativeTime?.take(animationUnit)?.sum() ?: 0L) +
            (animationFraction * (syncedLyric?.relativeTime[animationUnit] ?: 0L)).toLong()

    fun animate(itemPos: Int, fraction: Float) {
        animationFraction = fraction

        if (itemPos != animationUnit) {
            animationUnit = itemPos
            calculateTargetPosition(animationUnit)

            liftupStartupTimes?.let { starts ->
                if (itemPos in starts.indices) {
                    starts[itemPos] = progressSoFar
                }
            }
        }

        Log.d("TAG", "progressSoFar: $progressSoFar")
        calculateLiftupProgress()
        invalidate()
    }

    private fun calculateLiftupProgress() {
        val starts = liftupStartupTimes ?: return
        val progresses = liftupProgress ?: return

        val n = minOf(starts.size, progresses.size)
        val now = progressSoFar

        for (i in 0 until n) {
            val start = starts[i]
            if (start < 0L) {
                continue
            }
            val elapsed = now - start
            val p = when {
                elapsed <= 0L -> 0f
                elapsed >= LIFTUP_DURATION -> 1f
                else -> elapsed.toFloat() / LIFTUP_DURATION.toFloat()
            }
            progresses[i] = defaultPathInterpolator.getInterpolation(p)
        }
    }

    private fun calculateTargetPosition(itemPos: Int) {
        val list = syncedLyric?.list ?: return
        val layout = staticLayout ?: return

        val substringBeforeTarget = list.take(itemPos).joinToString("") { it.content }
        startOffsetChar = substringBeforeTarget.length
        endOffsetChar = startOffsetChar + list[itemPos].content.length

        startLine = layout.getLineForOffset(startOffsetChar)
        endLine = layout.getLineForOffset(endOffsetChar)

        startOffsetInLinePx = layout.getPrimaryHorizontal(startOffsetChar)
        endOffsetInLinePx = layout.getPrimaryHorizontal(endOffsetChar)

        // This is because the target has reached next
        // line but visually it does not, therefore we
        // need to recalculate the endOffset.
        if (endOffsetInLinePx == 0F) {
            endLine --
            endOffsetInLinePx = layout.getLineRight(endLine)
        }

        val lineArray = IntArray(endLine - startLine + 1)
        for (line in startLine..endLine) {
            if (startLine == endLine) {
                lineArray[0] = (endOffsetInLinePx - startOffsetInLinePx).toInt()
                break
            }
            if (line == startLine) {
                lineArray[0] = (layout.getLineRight(line) - layout.getLineLeft(line) - startOffsetInLinePx).toInt()
                continue
            }
            if (line == endLine) {
                lineArray[endLine - startLine] = endOffsetInLinePx.toInt()
                continue
            }
            lineArray[line - startLine] = (layout.getLineRight(line) - layout.getLineLeft(line)).toInt()
        }
        animationLinePx = LinePixels(lineArray)
    }

    // Start line of current synced lyrics.
    private var startLine = -1

    // End line of current synced lyrics.
    private var endLine = -1

    // Start offset to the left of current synced lyrics.
    private var startOffsetInLinePx = -1F

    // End offset to the left of current synced lyrics.
    private var endOffsetInLinePx = -1F

    // The string length of the subset of strings before
    // current synced lyrics.
    private var startOffsetChar = -1

    // The string length from the start of total lyrics
    // in this line till the end of the current synced
    // lyrics.
    private var endOffsetChar = -1

    // A helper class of current activated synced lyric
    // which reports the detailed progress of animating
    // lines
    private var animationLinePx: LinePixels? = null

    private val defaultPathInterpolator = PathInterpolator(0.2F, 0F, 0F, 1F)

    inner class LinePixels(
        val lineWidths: IntArray,
    ) {
        fun getTotalLineWidth() =
            lineWidths.sum()

        fun getLineAndProgress(animationFraction: Float): Pair<Int, Float> {
            val progressPixel = animationFraction * getTotalLineWidth()
            var accumulatingPixels = 0

            lineWidths.forEachIndexed { index, i ->
                if (accumulatingPixels + i < progressPixel) {
                    accumulatingPixels += i
                } else {
                    // We have found the target line
                    return Pair(startLine + index, progressPixel - accumulatingPixels)
                }
            }

            throw IllegalArgumentException("This is not supposed to be happening!")
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as LinePixels

            return lineWidths.contentEquals(other.lineWidths)
        }

        override fun hashCode(): Int {
            return lineWidths.contentHashCode()
        }
    }

    private fun drawContentLayerWithProgress(canvas: Canvas) {
        val startOffset = endOffsetChar
        val endOffset = lyric.length
        val layout = staticLayout ?: return

        val overlayAlpha =
            if (isHolding)
                HOLDING_OVERLAY_TRANSPARENCY
            else
                INACTIVE_OVERLAY_TRANSPARENCY

        val shadeAlpha =
            if (isHolding)
                HOLDING_SHADE_TRANSPARENCY
            else
                INACTIVE_SHADE_TRANSPARENCY

        val startLine = layout.getLineForOffset(startOffset)
        val endLine = layout.getLineForOffset(endOffset)

        for (line in startLine..endLine) {
            val lineTop = layout.getLineTop(line).toFloat()
            val lineBottom = layout.getLineBottom(line).toFloat()
            val lineLeft = layout.getLineLeft(line)
            val lineRight = layout.getLineRight(line)

            val startX = if (line == startLine) layout.getPrimaryHorizontal(startOffset) else lineLeft
            val endX = if (line == endLine) layout.getPrimaryHorizontal(endOffset) else lineRight

            canvas.withClip(startX, lineTop, endX, lineBottom) {
                staticLayout?.paint?.apply {
                    blendMode = BlendMode.OVERLAY
                    style = Paint.Style.FILL
                    alpha = getScopeAlpha(overlayAlpha)
                }
                staticLayout?.draw(this)

                staticLayout?.paint?.apply {
                    blendMode = null
                    style = Paint.Style.FILL
                    alpha = getScopeAlpha(shadeAlpha)
                }
                staticLayout?.draw(this)
            }
        }
    }

    private fun drawContentLayer(canvas: Canvas) {
        val overlayAlpha =
            if (isHolding)
                HOLDING_OVERLAY_TRANSPARENCY
            else
                INACTIVE_OVERLAY_TRANSPARENCY

        val shadeAlpha =
            if (isHolding)
                HOLDING_SHADE_TRANSPARENCY
            else
                INACTIVE_SHADE_TRANSPARENCY

        staticLayout.let { if (it == null) return@drawContentLayer }
        staticLayout?.paint?.apply {
            blendMode = BlendMode.OVERLAY
            style = Paint.Style.FILL
            alpha = getScopeAlpha(overlayAlpha)
        }
        staticLayout?.draw(canvas)

        staticLayout?.paint?.apply {
            blendMode = null
            style = Paint.Style.FILL
            alpha = getScopeAlpha(shadeAlpha)
        }
        staticLayout?.draw(canvas)
    }

    private fun drawHighlightLayer(canvas: Canvas) {
        // Draw already past line
        drawTextRange(canvas, animationUnit - 1)
        // Draw line that's animating
        drawHighlightText(canvas)
    }

    private fun drawTextRange(canvas: Canvas, endUnit: Int) {
        val layout = staticLayout ?: return
        val synced = syncedLyric ?: return
        val progress = liftupProgress ?: return

        if (endUnit !in synced.list.indices) return

        val shadeAlpha = ACTIVE_SHADE_TRANSPARENCY

        val lowerOverlayAlpha =
            if (isHolding)
                HOLDING_OVERLAY_TRANSPARENCY
            else
                INACTIVE_OVERLAY_TRANSPARENCY

        val lowerShadeAlpha =
            if (isHolding)
                HOLDING_SHADE_TRANSPARENCY
            else
                INACTIVE_SHADE_TRANSPARENCY

        var charOffset = 0

        for (i in synced.list.indices) {
            val content = synced.list[i].content
            val startOffset = charOffset
            val endOffset = charOffset + content.length
            charOffset = endOffset

            // Skip if not in range
            if (i > endUnit) continue

            val startLine = layout.getLineForOffset(startOffset)
            val endLine = layout.getLineForOffset(endOffset)

            for (line in startLine..endLine) {
                val lineTop = layout.getLineTop(line).toFloat()
                val lineBottom = layout.getLineBottom(line).toFloat()
                val lineLeft = layout.getLineLeft(line)
                val lineRight = layout.getLineRight(line)

                val startX = if (line == startLine) layout.getPrimaryHorizontal(startOffset) else lineLeft
                var endX = if (line == endLine) layout.getPrimaryHorizontal(endOffset) else lineRight

                // If endX is 0 because of newline
                if (endX == 0F && line < endLine) {
                    endX = lineRight
                }

                canvas.withClip(startX, lineTop, endX, lineBottom) {
                    this.translate(0F, -LIFTUP_PX * progress[i])

                    staticLayout?.paint?.apply {
                        blendMode = BlendMode.OVERLAY
                        style = Paint.Style.FILL
                        alpha = getScopeAlpha(lowerOverlayAlpha)
                    }
                    staticLayout?.draw(this)

                    staticLayout?.paint?.apply {
                        blendMode = null
                        style = Paint.Style.FILL
                        alpha = getScopeAlpha(lowerShadeAlpha)
                    }
                    staticLayout?.draw(this)

                    layout.paint.apply {
                        blendMode = null
                        strokeWidth = 0.5F
                        style = Paint.Style.FILL_AND_STROKE
                        alpha = getScopeAlpha(shadeAlpha)
                    }
                    staticLayout?.draw(this)
                }
            }
        }
    }

    private fun drawHighlightText(canvas: Canvas) {
        val layout = staticLayout ?: return
        val linePx = animationLinePx ?: return
        val progress = liftupProgress ?: return

        val shadeAlpha = ACTIVE_SHADE_TRANSPARENCY

        val (activeLine, pixelProgress) = linePx.getLineAndProgress(animationFraction)

        val lowerOverlayAlpha =
            if (isHolding)
                HOLDING_OVERLAY_TRANSPARENCY
            else
                INACTIVE_OVERLAY_TRANSPARENCY

        val lowerShadeAlpha =
            if (isHolding)
                HOLDING_SHADE_TRANSPARENCY
            else
                INACTIVE_SHADE_TRANSPARENCY

        for (line in activeLine..endLine) {
            val lineTop = layout.getLineTop(line).toFloat()
            val lineBottom = layout.getLineBottom(line).toFloat()
            val lineLeft = layout.getLineLeft(line)
            val lineRight = layout.getLineRight(line)

            val leftDist = if (line == startLine) startOffsetInLinePx else lineLeft
            val startX = if (line == activeLine) leftDist + pixelProgress else leftDist
            val endX = if (line == endLine) endOffsetInLinePx else lineRight
            canvas.withClip(startX, lineTop, endX, lineBottom) {
                canvas.translate(0F, -LIFTUP_PX * progress[animationUnit])
                staticLayout?.paint?.apply {
                    blendMode = BlendMode.OVERLAY
                    style = Paint.Style.FILL
                    alpha = getScopeAlpha(lowerOverlayAlpha)
                }
                staticLayout?.draw(this)

                staticLayout?.paint?.apply {
                    blendMode = null
                    style = Paint.Style.FILL
                    alpha = getScopeAlpha(lowerShadeAlpha)
                }
                staticLayout?.draw(this)
            }
        }

        for (line in startLine..activeLine) {
            val lineTop = layout.getLineTop(line).toFloat()
            val lineBottom = layout.getLineBottom(line).toFloat()
            val lineLeft = layout.getLineLeft(line)
            val lineRight = layout.getLineRight(line)

            val startX = if (line == startLine) startOffsetInLinePx else lineLeft
            val endX = if (line == activeLine) startX + pixelProgress else lineRight

            canvas.withClip(startX, lineTop, endX, lineBottom) {
                canvas.translate(0F, -LIFTUP_PX * progress[animationUnit])
                layout.paint.apply {
                    blendMode = null
                    strokeWidth = 0.5F
                    style = Paint.Style.FILL_AND_STROKE
                    alpha = getScopeAlpha(shadeAlpha)
                }
                layout.draw(this)
            }
        }

    }

    private var alpha = 1F

    override fun setAlpha(alpha: Float) {
        this.alpha = alpha
        invalidate()
    }

    override fun getAlpha(): Float = alpha

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)

        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val measuredWidth = when (widthMode) {
            MeasureSpec.EXACTLY -> widthSize
            MeasureSpec.AT_MOST -> widthSize
            MeasureSpec.UNSPECIFIED -> widthSize
            else -> widthSize
        }

        measureStaticLayout(lyric, measuredWidth)

        val desiredHeight = staticLayout?.height ?: 0

        val measuredHeight = when (heightMode) {
            MeasureSpec.EXACTLY -> heightSize
            MeasureSpec.AT_MOST -> desiredHeight.coerceAtMost(heightSize) + verticalMargin * 2
            MeasureSpec.UNSPECIFIED -> desiredHeight + verticalMargin * 2
            else -> desiredHeight
        }

        setMeasuredDimension(measuredWidth, measuredHeight)
    }

    fun release() {

    }

    private fun measureStaticLayout(value: String, measuredWidth: Int) {
        staticLayout = StaticLayout.Builder
            .obtain(
                value,
                0, value.length,
                lyricPaint,
                measuredWidth - 2 * horizontalMargin
            )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0F, 1F)
            .setIncludePad(false)
            .build()
    }

    private fun setCreatorContent() {
        lyricPaint.apply {
            textSize = BG_TEXT_SIZE.sp.px
            typeface = resources.getFont(R.font.inter_semibold)
        }
    }

    private fun getScopeAlpha(currentAlpha: Float): Int =
        (currentAlpha * 255 * this.alpha).toInt()

    companion object {
        const val ACTIVE_SHADE_TRANSPARENCY = .85F
        const val ACTIVE_OVERLAY_TRANSPARENCY = 1F

        const val INACTIVE_SHADE_TRANSPARENCY = .25F
        const val INACTIVE_OVERLAY_TRANSPARENCY = .3F

        const val HOLDING_SHADE_TRANSPARENCY = .45F
        const val HOLDING_OVERLAY_TRANSPARENCY = .75F

        const val NORMAL_TEXT_SIZE = 30
        const val BG_TEXT_SIZE = 24

        const val LIFTUP_DURATION = 700L
        const val LIFTUP_PX = 7
    }

}