package org.foedusprogramme.accordlyricimpl

import android.os.Bundle
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams

class MainActivity : AppCompatActivity() {

    private lateinit var lyricViewGroup: LyricViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        lyricViewGroup = findViewById(R.id.lyric)

        ViewCompat.setOnApplyWindowInsetsListener(lyricViewGroup) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                leftMargin = systemBars.left + v.marginLeft
                topMargin = systemBars.top + v.marginTop
                rightMargin = systemBars.right + v.marginRight
                bottomMargin = systemBars.bottom + v.marginBottom
            }
            insets
        }

        lyricViewGroup.updateLyrics(
            listOf(
                SyncedLyric(
                    listOf(
                        Lyric("啊 "),
                        Lyric("偏执"),
                        Lyric("是那"),
                        Lyric("玛格丽特 "),
                        Lyric("被我"),
                        Lyric("变出"),
                        Lyric("的"),
                        Lyric("苹"),
                        Lyric("果"),
                    ),
                    longArrayOf(
                        300,   // “啊 ”
                        300,   // “偏执”
                        400,   // “是那”
                        500,   // “玛格丽特 ”
                        400,   // “被我”
                        200,   // “变出”
                        100,   // “的”
                        300,   // “苹”
                        200,   // “果”
                    )
                ),
                Lyric("Are you going to Scarborough Fair"),
                Lyric("Parsley, sage, rosemary and thyme"),
                Lyric("Remember me to one who lives there"),
                Lyric("She once was a true love of mine"),
                Lyric("On the side of a hill in the deep forest green"),
                Lyric("Tracing of sparrow on snow-crested brown"),
                Lyric("Blankets and bedclothes the child of the mountain"),
                Lyric("Sleeps unaware of the clarion call"),
                Creator("Various Artists")
            )
        )
    }
}