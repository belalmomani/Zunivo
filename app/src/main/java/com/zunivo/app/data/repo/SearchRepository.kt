package com.zunivo.app.data.repo

import com.zunivo.app.data.remote.IconifyApi
import com.zunivo.app.domain.IconItem
import com.zunivo.app.domain.IconSetInfo
import com.zunivo.app.domain.SearchPage

data class SearchOutcome(val items: List<IconItem>, val hasMore: Boolean)

/** Runs up to a few related queries, merges the results and keeps a small in-memory cache. */
class SearchRepository(private val api: IconifyApi) {

    private val cache = object : LinkedHashMap<String, SearchPage>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, SearchPage>): Boolean =
            size > MAX_CACHED
    }

    suspend fun search(queries: List<String>, limit: Int): SearchOutcome {
        if (queries.isEmpty()) return SearchOutcome(emptyList(), false)
        val pages = queries.map { query -> page(query, limit) }
        val sets = HashMap<String, IconSetInfo>()
        pages.forEach { sets.putAll(it.sets) }

        val seen = HashSet<String>()
        val merged = ArrayList<IconItem>()
        val longest = pages.maxOf { it.icons.size }
        for (i in 0 until longest) {          // round-robin so every query contributes early results
            for (p in pages) {
                val ref = p.icons.getOrNull(i) ?: continue
                if (seen.add(ref.id)) merged.add(IconItem(ref, sets[ref.prefix]))
            }
        }
        val hasMore = limit < MAX_LIMIT && pages.any { it.limit > 0 && it.icons.size >= it.limit }
        return SearchOutcome(merged, hasMore)
    }

    private suspend fun page(query: String, limit: Int): SearchPage {
        val key = "$query|$limit"
        synchronized(cache) { cache[key] }?.let { return it }
        val fresh = api.search(query, limit)
        synchronized(cache) { cache[key] = fresh }
        return fresh
    }

    companion object {
        const val MAX_LIMIT = 999
        private const val MAX_CACHED = 40
    }
}
