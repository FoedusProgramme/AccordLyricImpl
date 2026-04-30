package org.foedusprogramme.accordlyricimpl

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.ViewGroup
import android.widget.ScrollView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.marginLeft
import androidx.core.view.marginRight
import androidx.core.view.marginTop
import androidx.core.view.updateLayoutParams
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var lyricViewGroup: LyricViewGroup
    private lateinit var scrollView: ScrollView
    private val lyricsUpdateHandler = Handler(Looper.getMainLooper())
    private var samplePlaybackStartUptimeMs = 0L
    private val lyricsUpdateRunnable = object : Runnable {
        override fun run() {
            val elapsedMs = SystemClock.uptimeMillis() - samplePlaybackStartUptimeMs
            val durationMs = lyricViewGroup.durationMs
            val positionMs = if (durationMs > 0L) elapsedMs % durationMs else elapsedMs

            lyricViewGroup.updatePlaybackPosition(scrollView, positionMs)
            lyricsUpdateHandler.postDelayed(this, LYRICS_UPDATE_INTERVAL_MS)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        scrollView = findViewById(R.id.scroll)
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
                sampleSyncedLyric("Are you going to Scarborough Fair"),
                sampleSyncedLyric("Parsley, sage, rosemary and thyme"),
                sampleSyncedLyric("Remember me to one who lives there"),
                sampleSyncedLyric("She once was a true love of mine"),
                sampleSyncedLyric("On the side of a hill in the deep forest green"),
                sampleSyncedLyric("Tracing of sparrow on snow-crested brown"),
                sampleSyncedLyric("Blankets and bedclothes the child of the mountain"),
                sampleSyncedLyric("Sleeps unaware of the clarion call"),
                Creator("Various Artists")
            )
        )

        lyricViewGroup.doOnLayout {
            lyricViewGroup.scrollViewHeight = scrollView.height
            startSampleLyricsUpdateThread()
        }
    }

    override fun onDestroy() {
        stopSampleLyricsUpdateThread()
        lyricViewGroup.stopPlayback()
        super.onDestroy()
    }

    private fun startSampleLyricsUpdateThread() {
        stopSampleLyricsUpdateThread()
        samplePlaybackStartUptimeMs = SystemClock.uptimeMillis()
        lyricsUpdateHandler.post(lyricsUpdateRunnable)
    }

    private fun stopSampleLyricsUpdateThread() {
        lyricsUpdateHandler.removeCallbacks(lyricsUpdateRunnable)
    }

    private fun sampleSyncedLyric(text: String): SyncedLyric =
        sampleSyncedLyric(
            *WORD_TOKEN_REGEX.findAll(text)
                .map { it.value }
                .toList()
                .toTypedArray()
        )

    private fun sampleSyncedLyric(vararg words: String): SyncedLyric =
        SyncedLyric(
            list = words.map(::Lyric),
            relativeTime = LongArray(words.size) {
                sampleDurationRandom.nextLong(
                    MIN_SAMPLE_WORD_DURATION_MS,
                    MAX_SAMPLE_WORD_DURATION_MS
                )
            }
        )

    companion object {
        private const val LYRICS_UPDATE_INTERVAL_MS = 16L
        private const val MIN_SAMPLE_WORD_DURATION_MS = 180L
        private const val MAX_SAMPLE_WORD_DURATION_MS = 920L
        private val WORD_TOKEN_REGEX = Regex("""\S+\s*""")
        private val sampleDurationRandom = Random(20260430)
    }
}
