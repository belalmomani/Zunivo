package com.zunivo.app.domain

/**
 * Preferences that stay on the phone. Learning only re-orders suggestions; it never hides anything.
 * Search history is opt-in and off by default.
 */
data class UserPrefs(
    val learnFromUsage: Boolean = true,
    val keepSearchHistory: Boolean = false,
    val categoryUse: Map<String, Int> = emptyMap(),
    val colorUse: Map<String, Int> = emptyMap(),
    val searchHistory: List<String> = emptyList(),
)

object PrefsOps {
    const val MAX_HISTORY = 20
    const val MAX_KEYS = 30

    private fun bump(map: Map<String, Int>, key: String): Map<String, Int> {
        val m = map.toMutableMap()
        m[key] = (m[key] ?: 0) + 1
        if (m.size > MAX_KEYS) {
            m.entries.filter { it.key != key }.minWithOrNull(compareBy({ it.value }, { it.key }))?.let { m.remove(it.key) }
        }
        return m
    }

    fun recordCategory(p: UserPrefs, name: String): UserPrefs =
        if (!p.learnFromUsage) p else p.copy(categoryUse = bump(p.categoryUse, name))

    fun recordColor(p: UserPrefs, hex: String?): UserPrefs {
        val h = HexColor.normalize(hex) ?: return p
        return if (!p.learnFromUsage) p else p.copy(colorUse = bump(p.colorUse, h))
    }

    fun recordSearch(p: UserPrefs, query: String): UserPrefs {
        if (!p.keepSearchHistory) return p
        val t = query.trim().replace(Regex("\\s+"), " ").take(60)
        if (t.length < 2) return p
        return p.copy(searchHistory = (listOf(t) + p.searchHistory.filter { !it.equals(t, ignoreCase = true) }).take(MAX_HISTORY))
    }

    fun topColors(p: UserPrefs, n: Int = 8): List<String> =
        p.colorUse.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(n).map { it.key }

    fun topCategories(p: UserPrefs, n: Int = 5): List<String> =
        p.categoryUse.entries.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key }).take(n).map { it.key }

    /** Most used first; everything else keeps its original order. Nothing is removed. */
    fun orderCategories(p: UserPrefs, all: List<String>): List<String> = all.sortedByDescending { p.categoryUse[it] ?: 0 }

    fun setLearning(p: UserPrefs, on: Boolean): UserPrefs = p.copy(learnFromUsage = on)

    /** Turning history off also deletes what was kept. */
    fun setHistory(p: UserPrefs, on: Boolean): UserPrefs =
        p.copy(keepSearchHistory = on, searchHistory = if (on) p.searchHistory else emptyList())

    fun removeHistory(p: UserPrefs, query: String): UserPrefs = p.copy(searchHistory = p.searchHistory.filter { it != query })
    fun clearHistory(p: UserPrefs): UserPrefs = p.copy(searchHistory = emptyList())
    fun clearLearned(p: UserPrefs): UserPrefs = p.copy(categoryUse = emptyMap(), colorUse = emptyMap())
    fun clearAll(): UserPrefs = UserPrefs()
}
