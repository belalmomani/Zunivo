package com.zunivo.app.feature.library

import com.zunivo.app.data.local.LibraryJson
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.Library
import com.zunivo.app.domain.LibraryOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsTest {
    private fun snap(name: String) = IconSnapshot(IconRef("mdi", name), "Set", "Author", "MIT License", "MIT", null)

    @Test fun newestFirstAndOpeningAgainMovesToFront() {
        var lib = Library()
        listOf("a", "b", "c").forEach { lib = LibraryOps.recordRecent(lib, snap(it)) }
        assertEquals(listOf("c", "b", "a"), lib.recents.map { it.icon.name })
        lib = LibraryOps.recordRecent(lib, snap("a"))
        assertEquals(listOf("a", "c", "b"), lib.recents.map { it.icon.name })
    }

    @Test fun listIsBounded() {
        var lib = Library()
        (1..30).forEach { lib = LibraryOps.recordRecent(lib, snap("i$it")) }
        assertEquals(LibraryOps.MAX_RECENTS, lib.recents.size)
        assertEquals("i30", lib.recents.first().icon.name)
    }

    @Test fun keepsSourceAndLicenseAndSurvivesStorage() {
        val lib = LibraryOps.recordRecent(Library(), snap("home"))
        val back = LibraryJson.fromJson(LibraryJson.toJson(lib))
        assertEquals(lib, back)
        assertEquals("MIT", back.recents.single().licenseSpdx)
    }

    @Test fun clearingOnlyRemovesRecents() {
        val lib = LibraryOps.toggleFavorite(LibraryOps.recordRecent(Library(), snap("home")), snap("star"))
        val cleared = LibraryOps.clearRecents(lib)
        assertTrue(cleared.recents.isEmpty())
        assertEquals(1, cleared.favorites.size)
    }

    @Test fun oldFilesWithoutRecentsStillLoad() {
        val old = """{"version":1,"favorites":[{"icon":"mdi:home"}],"collections":[]}"""
        val lib = LibraryJson.fromJson(old)
        assertEquals(1, lib.favorites.size)
        assertTrue(lib.recents.isEmpty())
    }
}
