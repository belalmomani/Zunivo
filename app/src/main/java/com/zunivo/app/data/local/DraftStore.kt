package com.zunivo.app.data.local

import android.content.Context
import com.zunivo.app.domain.StoreDraft
import java.io.File

/**
 * Drafts (JSON) and their downloaded files live in private storage under files/store.
 * Imported files are only ever stored, never executed.
 */
class DraftStore(context: Context) {
    private val root = File(context.filesDir, "store")
    private val drafts = File(root, "drafts")
    private val files = File(root, "files")

    @Synchronized
    fun loadAll(): List<StoreDraft> {
        val list = drafts.listFiles { f -> f.extension == "json" } ?: return emptyList()
        return list.mapNotNull { f ->
            try { DraftJson.fromJson(f.readText(Charsets.UTF_8)) } catch (e: Exception) { null }
        }.sortedByDescending { it.updatedAt }
    }

    @Synchronized
    fun save(d: StoreDraft) {
        drafts.mkdirs()
        val target = File(drafts, "${safeId(d.id)}.json")
        val tmp = File(drafts, "${safeId(d.id)}.json.tmp")
        tmp.writeText(DraftJson.toJson(d), Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8)
            tmp.delete()
        }
    }

    @Synchronized
    fun delete(id: String) {
        File(drafts, "${safeId(id)}.json").delete()
        deleteFiles(id)
    }

    fun filesDir(id: String): File = File(files, safeId(id)).apply { mkdirs() }

    @Synchronized
    fun deleteFiles(id: String) {
        File(files, safeId(id)).deleteRecursively()
    }

    /** Resolves a stored relative file name; refuses anything that would leave the draft's folder. */
    fun resolve(id: String, relative: String): File? {
        val base = File(files, safeId(id)).canonicalFile
        val f = File(base, relative).canonicalFile
        return if (f.path.startsWith(base.path + File.separator) && f.exists()) f else null
    }

    fun deleteOne(id: String, relative: String) {
        val base = File(files, safeId(id)).canonicalFile
        val f = File(base, relative).canonicalFile
        if (f.path.startsWith(base.path + File.separator)) f.delete()
    }

    private fun safeId(id: String): String = id.filter { it.isLetterOrDigit() || it == '-' }.ifEmpty { "x" }
}
