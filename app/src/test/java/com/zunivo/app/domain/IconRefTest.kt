package com.zunivo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IconRefTest {
    @Test fun parsesPrefixAndName() {
        val ref = IconRef.parse("mdi-light:home")
        assertEquals(IconRef("mdi-light", "home"), ref)
        assertEquals("mdi-light:home", ref?.id)
    }

    @Test fun rejectsMalformedAndUnsafeIds() {
        assertNull(IconRef.parse(null))
        assertNull(IconRef.parse("nocolon"))
        assertNull(IconRef.parse(":name"))
        assertNull(IconRef.parse("prefix:"))
        assertNull(IconRef.parse("a/b:home"))
        assertNull(IconRef.parse("mdi:../etc"))
        assertNull(IconRef.parse("mdi:ho me"))
    }

    @Test fun buildsSvgUrlWithEncodedColor() {
        val url = IconRef("mdi", "home").svgUrl(height = 48, colorHex = "#FFFFFF")
        assertEquals("https://api.iconify.design/mdi/home.svg?height=48&color=%23FFFFFF", url)
        assertEquals("https://api.iconify.design/mdi/home.svg", IconRef("mdi", "home").svgUrl())
    }
}
