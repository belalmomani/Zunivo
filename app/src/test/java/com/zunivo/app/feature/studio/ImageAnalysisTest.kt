package com.zunivo.app.feature.studio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageAnalysisTest {
    private val red = 0xFFFF0000.toInt()
    private val blue = 0xFF0000FF.toInt()

    @Test fun twoFlatColoursAreFound() {
        val px = IntArray(100) { if (it < 70) red else blue }
        val pal = PaletteExtractor.extract(px)
        assertEquals(2, pal.size)
        assertEquals(red, pal[0].argb)
        assertEquals(0.7f, pal[0].share, 0.001f)
        assertEquals(blue, pal[1].argb)
    }

    @Test fun nearbyShadesAreMerged() {
        val px = IntArray(100) { if (it % 2 == 0) 0xFFFF0000.toInt() else 0xFFF81010.toInt() }
        assertEquals(1, PaletteExtractor.extract(px).size)
    }

    @Test fun transparentPixelsAreIgnored() {
        assertTrue(PaletteExtractor.extract(IntArray(10) { 0x00FF0000 }).isEmpty())
    }

    @Test fun flatTwoToneImageIsSimple() {
        val w = 10; val h = 10
        val px = IntArray(w * h) { if (it % w < 5) red else blue }
        assertEquals(Simplicity.SIMPLE, ImageAnalyzer.analyze(px, w, h)!!.simplicity)
    }

    @Test fun noisyImageIsDetailed() {
        val w = 16; val h = 16
        val px = IntArray(w * h) { i -> if ((i + i / w) % 2 == 0) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
        assertEquals(Simplicity.DETAILED, ImageAnalyzer.analyze(px, w, h)!!.simplicity)
    }

    @Test fun invalidSizesGiveNull() {
        assertNull(ImageAnalyzer.analyze(IntArray(4), 0, 4))
        assertNull(ImageAnalyzer.analyze(IntArray(4), 4, 4))
    }

    @Test fun hslRoundTripAndHarmony() {
        val c = Harmony.fromHsl(Harmony.toHsl(red)[0], 1f, 0.5f)
        assertEquals(red, c)
        val palette = Harmony.build(red, null)
        assertNotNull(palette)
        assertEquals(red, palette.first())
        assertTrue(palette.size in 3..6)
        assertEquals(palette.size, palette.toSet().size)
    }
}
