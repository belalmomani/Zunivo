package com.zunivo.app.data.local

import android.content.Context
import com.zunivo.app.domain.Library
import java.io.File

/** Favourites, folders and packs live in private storage and survive restarts. No account needed. */
class LibraryStore(context: Context) {
    private val file = File(context.filesDir, "library.json")

    @Synchronized
    fun load(): Library {
        if (!file.exists()) return Library()
        return try {
            LibraryJson.fromJson(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            Library()   // unreadable file: start clean rather than crash
        }
    }

    @Synchronized
    fun save(lib: Library) {
        val tmp = File(file.parentFile, "library.json.tmp")
        tmp.writeText(LibraryJson.toJson(lib), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {
            file.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
    }
}
