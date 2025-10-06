package org.foedusprogramme.accordlyricimpl

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed

class LyricViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private val itemSpacing = ITEM_SPACING.dp.px.toInt()
    private val topPadding = TOP_PADDING.dp.px.toInt()

    private val lyrics: MutableList<LyricBase> = mutableListOf()

    fun updateLyrics(lyrics: List<LyricBase>) {

        this.lyrics.clear()
        this.lyrics.addAll(lyrics)

        forEach { child: View ->
            child as LyricTextView
            child.release()
        }
        removeAllViews()
        lyrics.forEachIndexed { index, line ->
            val view = LyricTextView(context, line)
            addView(view)
        }

    }

    var scrollViewHeight: Int = resources.displayMetrics.heightPixels
        set(value) {
            field = value
            requestLayout()
        }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        var top = topPadding
        forEachIndexed { index: Int, child: View ->
            val height = child.measuredHeight
            child.layout(0, top, child.measuredWidth, top + height)
            top += height + if (index != childCount - 1) itemSpacing else 0
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var totalHeight = topPadding
        var maxWidth = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)

            measureChild(child, widthMeasureSpec, heightMeasureSpec)

            totalHeight += child.measuredHeight + if (i != childCount - 1) itemSpacing else 0
            maxWidth = maxOf(maxWidth, child.measuredWidth)
        }

        val creatorHeight = if (lyrics.last() is Creator) getChildAt(childCount - 1).measuredHeight else 0
        val lastRealLyricHeight = if (lyrics.last() is Creator)
            getChildAt(childCount - 2).measuredHeight
        else
            getChildAt(childCount - 1).measuredHeight

        Log.d("TAG", "creatorHeight: $creatorHeight")

        totalHeight += scrollViewHeight - creatorHeight - lastRealLyricHeight - topPadding

        val width = resolveSize(maxWidth, widthMeasureSpec)
        val height = resolveSize(totalHeight, heightMeasureSpec)

        setMeasuredDimension(width, height)
    }

    companion object {
        const val ITEM_SPACING = 16
        const val TOP_PADDING = 48
    }

}