package org.foedusprogramme.accordlyricimpl

import android.content.Context
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import android.view.View
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withSave

class LyricTextView(
    context: Context,
) : View(context) {

    constructor(context: Context, lyric: LyricBase) : this(context) {
        when (lyric) {
            is Lyric -> { this.lyric = lyric.content }
            is SyncedLyric -> {
                this.syncedLyric = lyric
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
            drawContentLayer(canvas)
            if (syncedLyric != null) {
                drawHighlightLayer(canvas)
            }
        }
    }

    private var charProgress: FloatArray? = null
    private var itemStartOffsets: IntArray? = null

    private fun buildItemOffsets() {
        val synced = syncedLyric ?: return
        val offsets = IntArray(synced.list.size)
        var current = 0
        synced.list.forEachIndexed { i, lyric ->
            offsets[i] = current
            current += lyric.content.length
        }
        itemStartOffsets = offsets
        charProgress = FloatArray(synced.list.size) { 0f }
    }

    fun animate(itemPos: Int, fraction: Float) {
        val synced = syncedLyric ?: return

        if (charProgress == null || charProgress?.size != synced.list.size) {
            buildItemOffsets()
        }

        charProgress?.let {
            if (itemPos in it.indices) {
                it[itemPos] = fraction.coerceIn(0f, 1f)
            }
        }

        invalidate()
    }

    val paint = TextPaint().apply {
        textSize = NORMAL_TEXT_SIZE.sp.px
        color = Color.WHITE
        typeface = resources.getFont(R.font.inter_bold)
    }

    private fun drawHighlightLayer(canvas: Canvas) {
        val text = lyric
        val layout = staticLayout ?: return
        val progress = charProgress ?: return
        val offsets = itemStartOffsets ?: return
        val synced = syncedLyric ?: return

        for (line in 0 until layout.lineCount) {
            val start = layout.getLineStart(line)
            val end = layout.getLineEnd(line)
            var x = layout.getLineLeft(line)
            val baseY = layout.getLineBaseline(line).toFloat()

            for (itemIndex in synced.list.indices) {
                val itemStart = offsets[itemIndex]
                val itemText = synced.list[itemIndex].content
                val itemEnd = itemStart + itemText.length

                if (itemEnd <= start || itemStart >= end) continue

                val subStart = maxOf(start, itemStart)
                val subEnd = minOf(end, itemEnd)
                val visibleText = text.substring(subStart, subEnd)

                val width = paint.measureText(visibleText)
                val trailingWidth =
                    if (visibleText.endsWith(' '))
                        paint.measureText(visibleText.substringAfterLast(" ") + " ")
                    else
                        0F
                val finalWidth = width - trailingWidth

                val p = progress[itemIndex]

                if (p > 0f) {
                    canvas.withSave {
                        clipRect(x, baseY - paint.textSize, x + finalWidth * p, baseY + paint.descent())
                        drawText(visibleText, x, baseY, paint)
                    }
                }

                x += width
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
            alpha = (overlayAlpha * 255).toInt()
        }
        staticLayout?.draw(canvas)

        staticLayout?.paint?.apply {
            blendMode = null
            alpha = (shadeAlpha * 255).toInt()
        }
        staticLayout?.draw(canvas)
    }

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
        if (syncedLyric != null) {
            buildItemOffsets()
        }
    }

    private fun setCreatorContent() {
        lyricPaint.apply {
            textSize = BG_TEXT_SIZE.sp.px
            typeface = resources.getFont(R.font.inter_semibold)
        }
    }

    companion object {
        const val ACTIVE_SHADE_TRANSPARENCY = .85F
        const val ACTIVE_OVERLAY_TRANSPARENCY = 1F

        const val INACTIVE_SHADE_TRANSPARENCY = .25F
        const val INACTIVE_OVERLAY_TRANSPARENCY = .3F

        const val HOLDING_SHADE_TRANSPARENCY = .45F
        const val HOLDING_OVERLAY_TRANSPARENCY = .75F

        const val NORMAL_TEXT_SIZE = 34
        const val BG_TEXT_SIZE = 24
    }

}