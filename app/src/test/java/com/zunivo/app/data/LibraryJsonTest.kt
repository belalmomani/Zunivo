package com.zunivo.app.data

import com.zunivo.app.data.local.LibraryJson
import com.zunivo.app.domain.CollectionEntry
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.Library
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LibraryJsonTest {
    private val snap = IconSnapshot(IconRef("mdi", "home"), "Material", "Google", "Apache 2.0", "Apache-2.0", "https://x.example/l")
    private val lib = Library(
        favorites = listOf(snap),
        collections = listOf(
            IconCollection("c1", CollectionKind.PACK, "Pack", "d", listOf(CollectionEntry(snap, "home")), "#6366F1", 5L, 6L),
        ),
    )

    @Test fun roundTripKeepsLicenseAndOrder() {
        assertEquals(lib, LibraryJson.fromJson(LibraryJson.toJson(lib)))
    }

    @Test fun brokenEntriesAreSkippedAndFileNamesRepaired() {
        val json = """{"favorites":[{"icon":"bad id"},{"icon":"mdi:home"},{"icon":"mdi:home"}],
          "collections":[{"id":"","kind":"PACK"},{"id":"x","kind":"NOPE"},
          {"id":"ok","kind":"FOLDER","name":"","color":"nope","entries":[
            {"icon":"mdi:a","fileName":"../a"},{"icon":"mdi:b","fileName":"../a"}]}]}"""
        val l = LibraryJson.fromJson(json)
        assertEquals(1, l.favorites.size)                       // invalid and duplicate removed
        val c = l.collections.single()
        assertEquals("Untitled", c.name)
        assertNull(c.colorHex)
        assertEquals(listOf("a", "a-2"), c.entries.map { it.fileName })   // path tricks neutralised
    }
}
