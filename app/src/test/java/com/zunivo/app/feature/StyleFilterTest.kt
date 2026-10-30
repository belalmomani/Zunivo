package com.zunivo.app.feature

import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSetInfo
import com.zunivo.app.feature.search.IconStyle
import com.zunivo.app.feature.search.StyleFilter
import com.zunivo.app.feature.search.applyStyle
import org.junit.Assert.assertEquals
import org.junit.Test

class StyleFilterTest {
    private fun item(name: String, palette: Boolean? = null) = IconItem(
        IconRef("set", name),
        IconSetInfo("set", "Set", null, null, null, null, null, palette),
    )

    private val items = listOf(item("home-outline"), item("home-fill"), item("home"), item("smile", palette = true))

    @Test fun guessesFromNameTokens() {
        assertEquals(IconStyle.OUTLINE, IconStyle.guess("home-outline"))
        assertEquals(IconStyle.FILLED, IconStyle.guess("bell-solid"))
        assertEquals(IconStyle.UNKNOWN, IconStyle.guess("outlinebox")) // whole-token match only
    }

    @Test fun filtersApplyAsExpected() {
        assertEquals(4, items.applyStyle(StyleFilter.ALL).size)
        assertEquals(listOf("home-outline"), items.applyStyle(StyleFilter.OUTLINE).map { it.ref.name })
        assertEquals(listOf("home-fill"), items.applyStyle(StyleFilter.FILLED).map { it.ref.name })
        assertEquals(listOf("smile"), items.applyStyle(StyleFilter.COLORED).map { it.ref.name })
    }
}
