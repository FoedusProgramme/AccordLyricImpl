package org.foedusprogramme.accordlyricimpl

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach

class LyricViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val itemSpacing = ITEM_SPACING.dp.px.toInt()

    fun updateLyrics(lyrics: List<Lyric>) {
        forEach { child: View ->
            child as LyricTextView
            child.release()
        }
        removeAllViews()
        lyrics.forEachIndexed { index, line ->
            val view = LyricTextView(context, line)
            addView(view)
        }
        Log.d("TAG", "viewCount: $childCount")
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = 0
        forEach { child: View ->
            val height = child.measuredHeight
            child.layout(0, top, child.measuredWidth, top + height)
            top += height + itemSpacing
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var totalHeight = 0
        var maxWidth = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            totalHeight += child.measuredHeight + itemSpacing
            maxWidth = maxOf(maxWidth, child.measuredWidth)
        }

        val width = resolveSize(maxWidth, widthMeasureSpec)
        val height = resolveSize(totalHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    companion object {
        const val ITEM_SPACING = 10
    }

}