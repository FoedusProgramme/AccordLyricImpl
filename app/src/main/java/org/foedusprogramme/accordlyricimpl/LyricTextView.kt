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

class LyricTextView(
    context: Context,
) : View(context) {

    constructor(context: Context, lyric: LyricBase) : this(context) {
        when (lyric) {
            is Lyric -> { this.lyric = lyric.content }
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
        textSize = 38.sp.px
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

    var staticLayout: StaticLayout? = null
    var isHolding: Boolean = false

    override fun onDraw(canvas: Canvas) {
        canvas.withTranslation(
            horizontalMargin.toFloat(),
            verticalMargin.toFloat()
        ) {
            drawContentLayer(canvas)
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
        Log.d("TAG", "value: $value, $measuredWidth")
    }

    private fun setCreatorContent() {
        lyricPaint.apply {
            textSize = 24.sp.px
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
    }

}