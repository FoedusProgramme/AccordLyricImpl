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