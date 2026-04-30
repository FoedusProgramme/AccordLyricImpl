package org.foedusprogramme.accordlyricimpl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LyricTimingTest {

    @Test
    fun durationAtFallsBackAndCoercesInvalidDurations() {
        val relativeTime = longArrayOf(0L, -20L)

        assertEquals(1L, LyricTiming.durationAt(relativeTime, 0))
        assertEquals(1L, LyricTiming.durationAt(relativeTime, 1))
        assertEquals(350L, LyricTiming.durationAt(relativeTime, 2))
    }

    @Test
    fun linePositionUsesCompletedUnitsAndClampedFraction() {
        val relativeTime = longArrayOf(100L, 500L)

        assertEquals(350L, LyricTiming.linePositionMs(relativeTime, itemPos = 1, fraction = 0.5f))
        assertEquals(100L, LyricTiming.linePositionMs(relativeTime, itemPos = 1, fraction = -1f))
        assertEquals(600L, LyricTiming.linePositionMs(relativeTime, itemPos = 1, fraction = 2f))
    }

    @Test
    fun resolveUnitProgressFindsCurrentUnit() {
        val relativeTime = longArrayOf(100L, 500L, 250L)

        val first = LyricTiming.resolveUnitProgress(3, relativeTime, linePositionMs = 50L)
        assertEquals(0, first?.index)
        assertEquals(0.5f, first?.fraction ?: 0f, 0.0001f)

        val second = LyricTiming.resolveUnitProgress(3, relativeTime, linePositionMs = 250L)
        assertEquals(1, second?.index)
        assertEquals(0.3f, second?.fraction ?: 0f, 0.0001f)
    }

    @Test
    fun resolveUnitProgressKeepsLastUnitDuringLineHold() {
        val relativeTime = longArrayOf(100L, 500L)

        val progress = LyricTiming.resolveUnitProgress(2, relativeTime, linePositionMs = 1000L)

        assertEquals(1, progress?.index)
        assertEquals(1f, progress?.fraction ?: 0f, 0.0001f)
    }

    @Test
    fun resolveUnitProgressReturnsNullForEmptyLines() {
        assertNull(LyricTiming.resolveUnitProgress(0, longArrayOf(), linePositionMs = 100L))
    }

    @Test
    fun longUnitsUseDefaultLiftupWindow() {
        val relativeTime = longArrayOf(500L, 500L)

        val window = LyricTiming.liftupWindow(relativeTime, unitCount = 2, itemPos = 0)

        assertEquals(0L, window.startMs)
        assertEquals(700L, window.durationMs)
        assertEquals(700L, window.endMs)
    }

    @Test
    fun shortUnitDelaysUntilNextUnitLiftCanComplete() {
        val relativeTime = longArrayOf(100L, 500L)

        val window = LyricTiming.liftupWindow(relativeTime, unitCount = 2, itemPos = 0)

        assertEquals(100L, window.startMs)
        assertEquals(700L, window.durationMs)
        assertEquals(800L, window.endMs)
    }

    @Test
    fun veryShortUnitCanDelayThroughThirdUnit() {
        val relativeTime = longArrayOf(100L, 500L, 500L)

        val window = LyricTiming.liftupWindow(relativeTime, unitCount = 3, itemPos = 0)

        assertEquals(100L, window.startMs)
        assertEquals(1200L, window.durationMs)
        assertEquals(1300L, window.endMs)
    }

    @Test
    fun mediumShortUnitDelaysOneNeighborOnly() {
        val relativeTime = longArrayOf(300L, 300L, 300L)

        val window = LyricTiming.liftupWindow(relativeTime, unitCount = 3, itemPos = 0)

        assertEquals(300L, window.startMs)
        assertEquals(700L, window.durationMs)
        assertEquals(1000L, window.endMs)
    }

    @Test
    fun liftupWindowsAnalyzeTheWholeLine() {
        val relativeTime = longArrayOf(100L, 500L, 500L)

        val windows = LyricTiming.liftupWindows(relativeTime, unitCount = 3)

        assertEquals(100L, windows[0].startMs)
        assertEquals(100L, windows[1].startMs)
        assertEquals(600L, windows[2].startMs)
    }

    @Test
    fun activeGlowOnlyAppearsForUnitsLongerThanOneSecond() {
        val relativeTime = longArrayOf(1000L, 1001L)

        assertEquals(false, LyricTiming.shouldShowActiveGlow(relativeTime, itemPos = 0))
        assertEquals(true, LyricTiming.shouldShowActiveGlow(relativeTime, itemPos = 1))
    }

    @Test
    fun durationMsAddsHoldForSyncedLyrics() {
        val lyric = SyncedLyric(
            list = listOf(Lyric("one"), Lyric("two")),
            relativeTime = longArrayOf(100L, 200L)
        )

        assertEquals(1200L, LyricTiming.durationMs(lyric))
    }

    @Test
    fun durationMsUsesDefaultsForEmptyAndCreatorLines() {
        assertEquals(
            4200L,
            LyricTiming.durationMs(SyncedLyric(list = emptyList(), relativeTime = longArrayOf()))
        )
        assertEquals(3200L, LyricTiming.durationMs(Creator("Composer")))
        assertEquals(4200L, LyricTiming.durationMs(Lyric("plain line")))
    }
}
