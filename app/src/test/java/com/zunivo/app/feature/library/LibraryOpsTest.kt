package com.zunivo.app.feature.library

import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.FileNames
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.Library
import com.zunivo.app.domain.LibraryOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryOpsTest {
    private fun snap(name: String, spdx: String? = "MIT") =
        IconSnapshot(IconRef("mdi", name), "Set", "Author", "Lic", spdx, null)

    private fun lib() = LibraryOps.create(Library(), "c1", CollectionKind.FOLDER, "  My   folder ", "desc", 1L)

    @Test fun togglingFavoriteAddsThenRemoves() {
        val a = LibraryOps.toggleFavorite(Library(), snap("home"))
        assertEquals(1, a.favorites.size)
        assertEquals(0, LibraryOps.toggleFavorite(a, snap("home")).favorites.size)
    }

    @Test fun newestFavoriteComesFirst() {
        val l = LibraryOps.toggleFavorite(LibraryOps.toggleFavorite(Library(), snap("a")), snap("b"))
        assertEquals(listOf("b", "a"), l.favorites.map { it.icon.name })
    }

    @Test fun createCleansNameAndPutsNewestFirst() {
        val l = LibraryOps.create(lib(), "c2", CollectionKind.PACK, "Pack", "", 2L)
        assertEquals(listOf("c2", "c1"), l.collections.map { it.id })
        assertEquals("My folder", l.collections.last().name)
    }

    @Test fun addEntryIgnoresDuplicatesAndKeepsFileNamesUnique() {
        var c = lib().collections.first()
        c = LibraryOps.addEntry(c, snap("home"), 2L)
        val same = LibraryOps.addEntry(c, snap("home"), 3L)
        assertSame(c, same)
        c = LibraryOps.addEntry(c, IconSnapshot(IconRef("lucide", "home"), null, null, null, "MIT", null), 4L)
        assertEquals(listOf("home", "home-2"), c.entries.map { it.fileName })
    }

    @Test fun moveAndRemoveEntries() {
        var c = lib().collections.first()
        listOf("a", "b", "c").forEach { c = LibraryOps.addEntry(c, snap(it), 2L) }
        c = LibraryOps.moveEntry(c, 2, -1, 3L)
        assertEquals(listOf("a", "c", "b"), c.entries.map { it.snapshot.icon.name })
        assertSame(c, LibraryOps.moveEntry(c, 0, -1, 4L))           // out of range: unchanged
        c = LibraryOps.removeEntry(c, 1, 5L)
        assertEquals(listOf("a", "b"), c.entries.map { it.snapshot.icon.name })
    }

    @Test fun renamingFileSanitizesAndAvoidsClashes() {
        var c = lib().collections.first()
        listOf("a", "b").forEach { c = LibraryOps.addEntry(c, snap(it), 2L) }
        c = LibraryOps.renameEntryFile(c, 1, "a", 3L)
        assertEquals(listOf("a", "a-2"), c.entries.map { it.fileName })
        c = LibraryOps.renameEntryFile(c, 1, "../../evil name!", 4L)
        assertEquals("evil-name", c.entries[1].fileName)
    }

    @Test fun fileNameSanitizing() {
        assertEquals("icon", FileNames.sanitize("   "))
        assertEquals("icon", FileNames.sanitize("///"))
        assertEquals("a-b.c", FileNames.sanitize("a b.c"))
        assertTrue(FileNames.sanitize("x".repeat(200)).length <= 60)
    }

    @Test fun duplicateGetsNewIdAndSuffix() {
        val c = LibraryOps.addEntry(lib().collections.first(), snap("a"), 2L)
        val d = LibraryOps.duplicate(c, "c9", "(copy)", 9L)
        assertEquals("c9", d.id)
        assertEquals("My folder (copy)", d.name)
        assertEquals(c.entries, d.entries)
    }
}
