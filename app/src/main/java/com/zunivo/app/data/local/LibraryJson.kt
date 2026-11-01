package com.zunivo.app.data.local

import com.zunivo.app.domain.CollectionEntry
import com.zunivo.app.domain.CollectionKind
import com.zunivo.app.domain.FileNames
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.IconCollection
import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSnapshot
import com.zunivo.app.domain.Library
import org.json.JSONArray
import org.json.JSONObject

/** Pure JSON (de)serialisation of favourites, folders and packs. Bad entries are skipped, never fatal. */
object LibraryJson {
    private fun snapToJson(s: IconSnapshot) = JSONObject().apply {
        put("icon", s.icon.id)
        put("setName", s.setName ?: JSONObject.NULL)
        put("authorName", s.authorName ?: JSONObject.NULL)
        put("licenseTitle", s.licenseTitle ?: JSONObject.NULL)
        put("licenseSpdx", s.licenseSpdx ?: JSONObject.NULL)
        put("licenseUrl", s.licenseUrl ?: JSONObject.NULL)
    }

    private fun snapFromJson(o: JSONObject?): IconSnapshot? {
        if (o == null) return null
        val icon = IconRef.parse(o.optString("icon", null)) ?: return null
        fun str(key: String): String? = if (o.isNull(key)) null else o.optString(key, "").ifBlank { null }
        return IconSnapshot(icon, str("setName"), str("authorName"), str("licenseTitle"), str("licenseSpdx"), str("licenseUrl"))
    }

    fun toJson(lib: Library): String {
        val fav = JSONArray().also { a -> lib.favorites.forEach { a.put(snapToJson(it)) } }
        val cols = JSONArray()
        lib.collections.forEach { c ->
            cols.put(JSONObject().apply {
                put("id", c.id); put("kind", c.kind.name); put("name", c.name); put("description", c.description)
                put("color", c.colorHex ?: JSONObject.NULL)
                put("createdAt", c.createdAt); put("updatedAt", c.updatedAt)
                put("entries", JSONArray().also { arr ->
                    c.entries.forEach { e -> arr.put(snapToJson(e.snapshot).put("fileName", e.fileName)) }
                })
            })
        }
        return JSONObject().put("version", 1).put("favorites", fav).put("collections", cols).toString()
    }

    fun fromJson(text: String): Library {
        val root = JSONObject(text)
        val favorites = ArrayList<IconSnapshot>()
        root.optJSONArray("favorites")?.let { a ->
            for (i in 0 until a.length()) snapFromJson(a.optJSONObject(i))?.let { s ->
                if (favorites.none { it.icon == s.icon }) favorites.add(s)
            }
        }
        val collections = ArrayList<IconCollection>()
        root.optJSONArray("collections")?.let { a ->
            for (i in 0 until a.length()) {
                val o = a.optJSONObject(i) ?: continue
                val id = o.optString("id", "")
                if (id.isBlank()) continue
                val kind = runCatching { CollectionKind.valueOf(o.optString("kind", "")) }.getOrNull() ?: continue
                val entries = ArrayList<CollectionEntry>()
                o.optJSONArray("entries")?.let { ea ->
                    for (j in 0 until ea.length()) {
                        val eo = ea.optJSONObject(j)
                        val snap = snapFromJson(eo) ?: continue
                        if (entries.any { it.snapshot.icon == snap.icon }) continue
                        val file = FileNames.unique(FileNames.sanitize(eo?.optString("fileName", "") ?: ""), entries.map { it.fileName })
                        entries.add(CollectionEntry(snap, file))
                    }
                }
                collections.add(
                    IconCollection(
                        id = id, kind = kind, name = o.optString("name", "").ifBlank { "Untitled" },
                        description = o.optString("description", ""), entries = entries,
                        colorHex = HexColor.normalize(if (o.isNull("color")) null else o.optString("color", "")),
                        createdAt = o.optLong("createdAt", 0L), updatedAt = o.optLong("updatedAt", 0L),
                    ),
                )
            }
        }
        return Library(favorites, collections)
    }
}
