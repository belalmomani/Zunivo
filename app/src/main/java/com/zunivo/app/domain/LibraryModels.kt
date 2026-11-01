package com.zunivo.app.domain

/** Snapshot of an icon and its source/licence, kept with every favourite and collection entry. */
data class IconSnapshot(
    val icon: IconRef,
    val setName: String?,
    val authorName: String?,
    val licenseTitle: String?,
    val licenseSpdx: String?,
    val licenseUrl: String?,
) {
    fun toItem(): IconItem = IconItem(
        icon,
        IconSetInfo(icon.prefix, setName ?: icon.prefix, authorName, null, licenseTitle, licenseSpdx, licenseUrl, null),
    )

    companion object {
        fun of(item: IconItem): IconSnapshot = IconSnapshot(
            item.ref, item.set?.name, item.set?.authorName,
            item.set?.licenseTitle, item.set?.licenseSpdx, item.set?.licenseUrl,
        )
    }
}

enum class CollectionKind { FOLDER, PACK }

/** `fileName` is the name used inside exported archives (without extension). */
data class CollectionEntry(val snapshot: IconSnapshot, val fileName: String)

data class IconCollection(
    val id: String,
    val kind: CollectionKind,
    val name: String,
    val description: String,
    val entries: List<CollectionEntry>,
    val colorHex: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

data class Library(
    val favorites: List<IconSnapshot> = emptyList(),
    val collections: List<IconCollection> = emptyList(),
)

object FileNames {
    private val unsafe = Regex("[^A-Za-z0-9._-]+")

    /** Keeps only [A-Za-z0-9._-]; never empty, never starts with a dot, max 60 chars. */
    fun sanitize(raw: String): String {
        val cleaned = raw.trim().replace(unsafe, "-").trim('-', '.', '_').take(60).trim('-', '.', '_')
        return cleaned.ifEmpty { "icon" }
    }

    fun unique(base: String, taken: Collection<String>): String {
        val lower = taken.map { it.lowercase() }.toSet()
        if (base.lowercase() !in lower) return base
        var n = 2
        while ("$base-$n".lowercase() in lower) n++
        return "$base-$n"
    }
}

/** Pure list/collection operations (no Android, fully unit-tested). */
object LibraryOps {
    const val MAX_ENTRIES = 200
    const val MAX_NAME = 60

    fun cleanName(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").take(MAX_NAME)

    fun toggleFavorite(lib: Library, snap: IconSnapshot): Library {
        val exists = lib.favorites.any { it.icon == snap.icon }
        val next = if (exists) lib.favorites.filter { it.icon != snap.icon } else listOf(snap) + lib.favorites
        return lib.copy(favorites = next)
    }

    fun create(lib: Library, id: String, kind: CollectionKind, name: String, description: String, now: Long): Library {
        val c = IconCollection(id, kind, cleanName(name).ifEmpty { "Untitled" }, description.trim().take(300), emptyList(), null, now, now)
        return lib.copy(collections = listOf(c) + lib.collections)
    }

    fun update(lib: Library, id: String, change: (IconCollection) -> IconCollection): Library =
        lib.copy(collections = lib.collections.map { if (it.id == id) change(it) else it })

    /** Returns null-change (same object) when the icon is already there or the collection is full. */
    fun addEntry(c: IconCollection, snap: IconSnapshot, now: Long): IconCollection {
        if (c.entries.any { it.snapshot.icon == snap.icon } || c.entries.size >= MAX_ENTRIES) return c
        val name = FileNames.unique(FileNames.sanitize(snap.icon.name), c.entries.map { it.fileName })
        return c.copy(entries = c.entries + CollectionEntry(snap, name), updatedAt = now)
    }

    fun removeEntry(c: IconCollection, index: Int, now: Long): IconCollection =
        if (index !in c.entries.indices) c else c.copy(entries = c.entries.filterIndexed { i, _ -> i != index }, updatedAt = now)

    fun moveEntry(c: IconCollection, index: Int, delta: Int, now: Long): IconCollection {
        val target = index + delta
        if (index !in c.entries.indices || target !in c.entries.indices) return c
        val list = c.entries.toMutableList()
        val item = list.removeAt(index)
        list.add(target, item)
        return c.copy(entries = list, updatedAt = now)
    }

    fun renameEntryFile(c: IconCollection, index: Int, raw: String, now: Long): IconCollection {
        if (index !in c.entries.indices) return c
        val others = c.entries.filterIndexed { i, _ -> i != index }.map { it.fileName }
        val name = FileNames.unique(FileNames.sanitize(raw), others)
        val list = c.entries.toMutableList()
        list[index] = list[index].copy(fileName = name)
        return c.copy(entries = list, updatedAt = now)
    }

    fun duplicate(c: IconCollection, newId: String, copySuffix: String, now: Long): IconCollection =
        c.copy(id = newId, name = cleanName("${c.name} $copySuffix"), createdAt = now, updatedAt = now)
}
