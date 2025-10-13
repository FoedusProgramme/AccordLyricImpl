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
                SyncedLyric(listOf(
                    /*
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                    Lyric("TEST "),
                     */
                    Lyric("你知道吗："),
                    Lyric("这段文"),
                    Lyric("字其实没有"),
                    Lyric("任何的意义，啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊啊"),
                    Lyric("它存在的意义便是"),
                    Lyric("为了多行歌"),
                    Lyric("词的进度"),
                    Lyric("测试，因"),
                    Lyric("此你可以完全"),
                    Lyric("地无视它。"),
                    /*
                    Lyric("I "),
                    Lyric("don't "),
                    Lyric("need "),
                    Lyric("no "),
                    Lyric("light "),
                    Lyric("to "),
                    Lyric("see "),
                    Lyric("you "),
                    Lyric("shine "),
                     */
                )),
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