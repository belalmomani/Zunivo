package com.zunivo.app.data.local

import android.content.Context
import com.zunivo.app.domain.HexColor
import com.zunivo.app.domain.PrefsOps
import com.zunivo.app.domain.UserPrefs
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object PrefsJson {
    private fun mapToJson(m: Map<String, Int>) = JSONObject().also { o -> m.forEach { (k, v) -> o.put(k, v) } }

    private fun jsonToMap(o: JSONObject?, keyOk: (String) -> Boolean): Map<String, Int> {
        if (o == null) return emptyMap()
        val out = LinkedHashMap<String, Int>()
        val keys = o.keys()
        while (keys.hasNext()) {
            val k = keys.next()
            val v = o.optInt(k, 0)
            if (v > 0 && keyOk(k) && out.size < PrefsOps.MAX_KEYS) out[k] = v
        }
        return out
    }

    fun toJson(p: UserPrefs): String = JSONObject().apply {
        put("version", 1)
        put("learn", p.learnFromUsage)
        put("history", p.keepSearchHistory)
        put("categories", mapToJson(p.categoryUse))
        put("colors", mapToJson(p.colorUse))
        put("searches", JSONArray(p.searchHistory))
    }.toString()

    fun fromJson(text: String): UserPrefs {
        val o = JSONObject(text)
        val history = ArrayList<String>()
        val keep = o.optBoolean("history", false)
        o.optJSONArray("searches")?.let { a ->
            for (i in 0 until a.length()) a.optString(i).trim().takeIf { it.length in 2..60 }?.let { history.add(it) }
        }
        return UserPrefs(
            learnFromUsage = o.optBoolean("learn", true),
            keepSearchHistory = keep,
            categoryUse = jsonToMap(o.optJSONObject("categories")) { it.matches(Regex("[A-Z_]{1,30}")) },
            colorUse = jsonToMap(o.optJSONObject("colors")) { HexColor.isValid(it) },
            searchHistory = if (keep) history.take(PrefsOps.MAX_HISTORY) else emptyList(),
        )
    }
}

class PrefsStore(context: Context) {
    private val file = File(context.filesDir, "prefs.json")

    @Synchronized
    fun load(): UserPrefs {
        if (!file.exists()) return UserPrefs()
        return try { PrefsJson.fromJson(file.readText(Charsets.UTF_8)) } catch (e: Exception) { UserPrefs() }
    }

    @Synchronized
    fun save(p: UserPrefs) {
        val tmp = File(file.parentFile, "prefs.json.tmp")
        tmp.writeText(PrefsJson.toJson(p), Charsets.UTF_8)
        if (!tmp.renameTo(file)) { file.writeText(tmp.readText(Charsets.UTF_8), Charsets.UTF_8); tmp.delete() }
    }
}
