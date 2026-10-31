package com.zunivo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditParamsTest {
    private val ref = IconRef("mdi", "home")

    @Test fun defaultUrlHasNoQuery() {
        assertEquals("https://api.iconify.design/mdi/home.svg", EditParams().svgUrl(ref))
        assertTrue(EditParams().isDefault())
    }

    @Test fun urlCarriesColorRotateFlip() {
        val p = EditParams(colorHex = "#6366F1", rotation = 90, flipH = true, flipV = true)
        assertEquals(
            "https://api.iconify.design/mdi/home.svg?height=64&color=%236366F1&rotate=1&flip=horizontal,vertical",
            p.svgUrl(ref, height = 64),
        )
    }

    @Test fun sanitizedClampsAndNormalizes() {
        val p = EditParams(colorHex = "6366f1", bgHex = "nope", sizePx = 5000, paddingPct = 99, rotation = -90).sanitized()
        assertEquals("#6366F1", p.colorHex)
        assertNull(p.bgHex)
        assertEquals(EditParams.MAX_SIZE, p.sizePx)
        assertEquals(EditParams.MAX_PADDING, p.paddingPct)
        assertEquals(270, p.rotation)
    }

    @Test fun hexHelpers() {
        assertEquals("#FF8800", HexColor.fromArgb(0xFFFF8800.toInt()))
        assertNull(HexColor.normalize("#12345"))
    }
}
