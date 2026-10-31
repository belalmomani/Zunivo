package com.zunivo.app.data.local

import android.content.Context
import com.zunivo.app.domain.IconEdit
import java.io.File

/** Saved edits live in the app's private storage (no permission, removed with app data). */
class EditStore(context: Context) {
    private val file = File(context.filesDir, "edits.json")

    @Synchronized
    fun load(): List<IconEdit> {
        if (!file.exists()) return emptyList()
        return try {
            EditJson.fromJson(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            emptyList()   // corrupt file: start clean instead of crashing
        }
    }

    @Synchronized
    fun save(edits: List<IconEdit>) {
        val tmp = File(file.parentFile, "edits.json.tmp")
        tmp.writeText(EditJson.toJson(edits), Charsets.UTF_8)
        if (!tmp.renameTo(file)) {   // rename can fail on some filesystems
            file.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
    }
}
