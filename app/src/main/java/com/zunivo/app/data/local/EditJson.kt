package com.zunivo.app.data.local

import com.zunivo.app.domain.EditParams
import com.zunivo.app.domain.IconEdit
import com.zunivo.app.domain.IconRef
import org.json.JSONArray
import org.json.JSONObject

/** Pure JSON (de)serialisation of saved edits; invalid entries are skipped rather than crashing the app. */
object EditJson {
    fun toJson(edits: List<IconEdit>): String {
        val arr = JSONArray()
        edits.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("icon", e.icon.id)
                put("setName", e.setName ?: JSONObject.NULL)
                put("authorName", e.authorName ?: JSONObject.NULL)
                put("licenseTitle", e.licenseTitle ?: JSONObject.NULL)
                put("licenseSpdx", e.licenseSpdx ?: JSONObject.NULL)
                put("licenseUrl", e.licenseUrl ?: JSONObject.NULL)
                put("color", e.params.colorHex ?: JSONObject.NULL)
                put("bg", e.params.bgHex ?: JSONObject.NULL)
                put("size", e.params.sizePx)
                put("padding", e.params.paddingPct)
                put("rotation", e.params.rotation)
                put("flipH", e.params.flipH)
                put("flipV", e.params.flipV)
                put("createdAt", e.createdAt)
                put("updatedAt", e.updatedAt)
            })
        }
        return JSONObject().put("version", 1).put("edits", arr).toString()
    }

    fun fromJson(text: String): List<IconEdit> {
        val arr = JSONObject(text).optJSONArray("edits") ?: return emptyList()
        val out = ArrayList<IconEdit>()
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val icon = IconRef.parse(o.optString("icon", null)) ?: continue
            val id = o.optString("id", "")
            if (id.isBlank()) continue
            fun str(key: String): String? = if (o.isNull(key)) null else o.optString(key, "").ifBlank { null }
            out.add(
                IconEdit(
                    id = id, icon = icon,
                    setName = str("setName"), authorName = str("authorName"),
                    licenseTitle = str("licenseTitle"), licenseSpdx = str("licenseSpdx"), licenseUrl = str("licenseUrl"),
                    params = EditParams(
                        colorHex = str("color"), bgHex = str("bg"),
                        sizePx = o.optInt("size", 256), paddingPct = o.optInt("padding", 10),
                        rotation = o.optInt("rotation", 0),
                        flipH = o.optBoolean("flipH", false), flipV = o.optBoolean("flipV", false),
                    ).sanitized(),
                    createdAt = o.optLong("createdAt", 0L), updatedAt = o.optLong("updatedAt", 0L),
                )
            )
        }
        return out
    }
}
