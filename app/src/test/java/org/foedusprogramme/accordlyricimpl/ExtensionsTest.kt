package org.foedusprogramme.accordlyricimpl

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun applyAlphaClampsAlphaAndPreservesRgb() {
        assertEquals(0x00123456, 0xFF123456.toInt().applyAlpha(-1))
        assertEquals(0x80123456.toInt(), 0x00123456.applyAlpha(128))
        assertEquals(0xFF123456.toInt(), 0x00123456.applyAlpha(999))
    }

    @Test
    fun trianglePeaksAtMiddleAndClampsInput() {
        assertEquals(0f, triangle(-1f), 0.0001f)
        assertEquals(0f, triangle(0f), 0.0001f)
        assertEquals(0.5f, triangle(0.25f), 0.0001f)
        assertEquals(1f, triangle(0.5f), 0.0001f)
        assertEquals(0.5f, triangle(0.75f), 0.0001f)
        assertEquals(0f, triangle(2f), 0.0001f)
    }
}
