package com.zunivo.app.feature.prefs

import com.zunivo.app.data.local.PrefsJson
import com.zunivo.app.domain.PrefsOps
import com.zunivo.app.domain.UserPrefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PrefsTest {
    @Test fun searchHistoryIsOffByDefaultAndOptIn() {
        val off = PrefsOps.recordSearch(UserPrefs(), "home")
        assertTrue(off.searchHistory.isEmpty())
        val on = PrefsOps.recordSearch(PrefsOps.setHistory(UserPrefs(), true), "  home   icon ")
        assertEquals(listOf("home icon"), on.searchHistory)
    }

    @Test fun historyDedupesCapsAndIgnoresTinyQueries() {
        var p = PrefsOps.setHistory(UserPrefs(), true)
        p = PrefsOps.recordSearch(p, "a")
        assertTrue(p.searchHistory.isEmpty())
        (1..30).forEach { p = PrefsOps.recordSearch(p, "query $it") }
        assertEquals(PrefsOps.MAX_HISTORY, p.searchHistory.size)
        assertEquals("query 30", p.searchHistory.first())
        p = PrefsOps.recordSearch(p, "QUERY 25")
        assertEquals("QUERY 25", p.searchHistory.first())
        assertEquals(PrefsOps.MAX_HISTORY, p.searchHistory.size)
    }

    @Test fun turningHistoryOffDeletesIt() {
        val p = PrefsOps.recordSearch(PrefsOps.setHistory(UserPrefs(), true), "home")
        assertTrue(PrefsOps.setHistory(p, false).searchHistory.isEmpty())
    }

    @Test fun learningCanBeSwitchedOff() {
        val p = PrefsOps.setLearning(UserPrefs(), false)
        assertSame(p, PrefsOps.recordCategory(p, "TECH"))
        assertSame(p, PrefsOps.recordColor(p, "#6366F1"))
    }

    @Test fun categoriesAreReorderedNeverRemoved() {
        var p = UserPrefs()
        repeat(3) { p = PrefsOps.recordCategory(p, "FOOD") }
        p = PrefsOps.recordCategory(p, "TECH")
        val all = listOf("TECH", "BUSINESS", "FOOD", "NATURE")
        assertEquals(listOf("FOOD", "TECH", "BUSINESS", "NATURE"), PrefsOps.orderCategories(p, all))
        assertEquals(all.toSet(), PrefsOps.orderCategories(p, all).toSet())
        assertEquals(listOf("FOOD", "TECH"), PrefsOps.topCategories(p, 2))
    }

    @Test fun colorsAreNormalizedAndInvalidOnesIgnored() {
        var p = PrefsOps.recordColor(UserPrefs(), "6366f1")
        p = PrefsOps.recordColor(p, "#6366F1")
        p = PrefsOps.recordColor(p, "nope")
        p = PrefsOps.recordColor(p, null)
        assertEquals(mapOf("#6366F1" to 2), p.colorUse)
        assertEquals(listOf("#6366F1"), PrefsOps.topColors(p))
    }

    @Test fun learnedDataIsBounded() {
        var p = UserPrefs()
        (0 until 100).forEach { i -> p = PrefsOps.recordColor(p, "#%06X".format(i)) }
        assertTrue(p.colorUse.size <= PrefsOps.MAX_KEYS)
    }

    @Test fun clearOperations() {
        var p = PrefsOps.setHistory(UserPrefs(), true)
        p = PrefsOps.recordSearch(PrefsOps.recordCategory(PrefsOps.recordColor(p, "#FFFFFF"), "TECH"), "home")
        assertTrue(PrefsOps.clearLearned(p).categoryUse.isEmpty() && PrefsOps.clearLearned(p).colorUse.isEmpty())
        assertEquals(1, PrefsOps.clearLearned(p).searchHistory.size)
        assertTrue(PrefsOps.clearHistory(p).searchHistory.isEmpty())
        assertEquals(UserPrefs(), PrefsOps.clearAll())
    }

    @Test fun jsonRoundTripAndSanitizing() {
        var p = PrefsOps.setHistory(UserPrefs(), true)
        p = PrefsOps.recordSearch(PrefsOps.recordCategory(PrefsOps.recordColor(p, "#10B981"), "FOOD"), "coffee")
        assertEquals(p, PrefsJson.fromJson(PrefsJson.toJson(p)))
        val dirty = """{"learn":true,"history":false,"categories":{"TECH":2,"bad key!":5,"FOOD":0},
            "colors":{"#6366F1":1,"zzz":9},"searches":["secret","x"]}"""
        val clean = PrefsJson.fromJson(dirty)
        assertEquals(mapOf("TECH" to 2), clean.categoryUse)
        assertEquals(mapOf("#6366F1" to 1), clean.colorUse)
        assertFalse(clean.searchHistory.isNotEmpty())   // history flag off => nothing is restored
    }
}
