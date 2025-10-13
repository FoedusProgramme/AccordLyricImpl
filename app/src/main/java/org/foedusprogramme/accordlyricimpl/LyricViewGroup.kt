package org.foedusprogramme.accordlyricimpl

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.postDelayed

class LyricViewGroup @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    init {
        postDelayed(1000) {
            val targetView = getChildAt(0) as LyricTextView
            val synced = lyrics[0] as SyncedLyric
            playLyricsSequentially(targetView, synced)
        }
    }

    /*
    *
    *                 <span begin="01:30.120" end="01:30.930">I </span>
                <span begin="01:30.930" end="01:31.350">don't </span>
                <span begin="01:31.350" end="01:31.620">need </span>
                <span begin="01:31.620" end="01:31.890">no </span>
                <span begin="01:31.890" end="01:32.790">light </span>
                <span begin="01:32.820" end="01:33.690">to </span>
                <span begin="01:33.690" end="01:34.800">see </span>
                <span begin="01:34.800" end="01:35.850">you </span>
                <span ttm:role="x-bg">
                    <span begin="01:31.890" end="01:32.790">light </span>
                    <span begin="01:32.820" end="01:33.690">to </span>
                    <span begin="01:33.690" end="01:34.800">see </span>
                    <span begin="01:34.800" end="01:35.850">you </span>
                </span>
            </p>
            <p begin="01:36.480" end="01:39.630" ttm:agent="v1" itunes:key="L34">
                <span begin="01:36.480" end="01:39.630">Shine </span>
    *
    * */

    private val durationMap: LongArray = longArrayOf(
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
        5000,
    )

    // TEST ONLY REMOVE IN PROD
    fun playLyricsSequentially(targetView: LyricTextView, lyrics: SyncedLyric) {
        fun playNext(index: Int) {
            if (index >= lyrics.list.size) return

            ValueAnimator.ofFloat(0f, 1f).apply {
                duration = durationMap[index]
                addUpdateListener {
                    targetView.animate(index, animatedValue as Float)
                }
                addListener(object : android.animation.Animator.AnimatorListener {
                    override fun onAnimationStart(animation: android.animation.Animator) {}
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        playNext(index + 1)
                    }
                    override fun onAnimationCancel(animation: android.animation.Animator) {}
                    override fun onAnimationRepeat(animation: android.animation.Animator) {}
                })
                start()
            }
        }

        playNext(0)
    }


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