package com.zunivo.app.feature

import com.zunivo.app.feature.search.QueryExpander
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueryExpanderTest {
    @Test fun tooShortGivesNothing() {
        assertTrue(QueryExpander.expand("").isEmpty())
        assertTrue(QueryExpander.expand("a").isEmpty())
    }

    @Test fun englishKeepsQueryAndAddsSynonym() {
        assertEquals(listOf("home", "house"), QueryExpander.expand("Home"))
        assertEquals(listOf("rocket"), QueryExpander.expand("  rocket "))
    }

    @Test fun arabicIsTranslatedWithDictionary() {
        assertEquals("home", QueryExpander.expand("بيت").first())
        assertEquals("home", QueryExpander.expand("البيت").first())      // definite article
        assertEquals("settings", QueryExpander.expand("إعدادات").first()) // hamza normalised
    }

    @Test fun unknownArabicGivesNothing() {
        assertTrue(QueryExpander.expand("كلمةغريبة").isEmpty())
    }

    @Test fun neverMoreThanThreeQueries() {
        assertTrue(QueryExpander.expand("بيت مستخدم سيارة مطعم").size <= 3)
    }
}
