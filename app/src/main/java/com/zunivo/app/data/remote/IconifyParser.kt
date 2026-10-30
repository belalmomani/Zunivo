package com.zunivo.app.data.remote

import com.zunivo.app.domain.IconRef
import com.zunivo.app.domain.IconSetInfo
import com.zunivo.app.domain.SearchPage
import org.json.JSONArray
import org.json.JSONObject

/** Pure parsing of Iconify `/search` JSON. Throws org.json.JSONException on malformed input. */
object IconifyParser {
    fun parseSearch(json: String): SearchPage {
        val root = JSONObject(json)
        val arr = root.optJSONArray("icons") ?: JSONArray()
        val icons = ArrayList<IconRef>(arr.length())
        for (i in 0 until arr.length()) {
            IconRef.parse(arr.optString(i, null))?.let { icons.add(it) }
        }
        val sets = HashMap<String, IconSetInfo>()
        val collections = root.optJSONObject("collections")
        if (collections != null) {
            val keys = collections.keys()
            while (keys.hasNext()) {
                val prefix = keys.next()
                val obj = collections.optJSONObject(prefix) ?: continue
                sets[prefix] = parseSet(prefix, obj)
            }
        }
        return SearchPage(icons, root.optInt("total", icons.size), root.optInt("limit", 0), sets)
    }

    private fun parseSet(prefix: String, obj: JSONObject): IconSetInfo {
        val author = obj.optJSONObject("author")
        val license = obj.optJSONObject("license")
        return IconSetInfo(
            prefix = prefix,
            name = obj.optString("name", prefix).ifBlank { prefix },
            authorName = author?.optString("name", "")?.ifBlank { null },
            authorUrl = author?.optString("url", "")?.ifBlank { null },
            licenseTitle = license?.optString("title", "")?.ifBlank { null },
            licenseSpdx = license?.optString("spdx", "")?.ifBlank { null },
            licenseUrl = license?.optString("url", "")?.ifBlank { null },
            palette = if (obj.has("palette")) obj.optBoolean("palette") else null,
        )
    }
}
