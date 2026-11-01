package com.zunivo.app.data.local

import com.zunivo.app.domain.Decision
import com.zunivo.app.domain.DraftItem
import com.zunivo.app.domain.FetchStatus
import com.zunivo.app.domain.ImportMode
import com.zunivo.app.domain.ItemKind
import com.zunivo.app.domain.ItemNote
import com.zunivo.app.domain.RepoRef
import com.zunivo.app.domain.StoreDraft
import com.zunivo.app.domain.Verification
import org.json.JSONArray
import org.json.JSONObject

/** Pure JSON for one draft. Unknown enum values make the entry fall back to safe defaults. */
object DraftJson {
    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: default

    private fun JSONObject.optStr(key: String): String? = if (isNull(key)) null else optString(key, "").ifEmpty { null }

    fun toJson(d: StoreDraft): String {
        val items = JSONArray()
        d.items.forEach { i ->
            items.put(JSONObject().apply {
                put("id", i.id); put("kind", i.kind.name); put("label", i.label)
                put("value", i.value ?: JSONObject.NULL); put("fetch", i.fetch.name); put("decision", i.decision.name)
                put("sourceUrl", i.sourceUrl ?: JSONObject.NULL)
                put("size", i.sizeBytes ?: JSONObject.NULL)
                put("localFile", i.localFile ?: JSONObject.NULL)
                put("expectedSha256", i.expectedSha256 ?: JSONObject.NULL)
                put("computedSha256", i.computedSha256 ?: JSONObject.NULL)
                put("verification", i.verification?.name ?: JSONObject.NULL)
                put("note", i.note?.name ?: JSONObject.NULL)
                put("changed", i.changed)
            })
        }
        return JSONObject().apply {
            put("version", 1); put("id", d.id); put("owner", d.repo.owner); put("repo", d.repo.repo)
            put("createdAt", d.createdAt); put("updatedAt", d.updatedAt)
            put("approved", d.approved); put("approvedAt", d.approvedAt); put("mode", d.mode.name)
            put("sizeKb", d.sizeKb ?: JSONObject.NULL); put("defaultBranch", d.defaultBranch)
            put("displayName", d.displayName); put("displayDescription", d.displayDescription)
            put("categories", JSONArray(d.categories)); put("cover", d.coverItemId ?: JSONObject.NULL)
            put("items", items)
        }.toString()
    }

    fun fromJson(text: String): StoreDraft {
        val o = JSONObject(text)
        val items = ArrayList<DraftItem>()
        val arr = o.optJSONArray("items") ?: JSONArray()
        for (n in 0 until arr.length()) {
            val i = arr.optJSONObject(n) ?: continue
            val id = i.optString("id", "")
            if (id.isEmpty() || items.any { it.id == id }) continue
            items.add(
                DraftItem(
                    id = id,
                    kind = enumOr(i.optString("kind"), ItemKind.README),
                    label = i.optString("label", id),
                    value = i.optStr("value"),
                    fetch = enumOr(i.optString("fetch"), FetchStatus.FAILED),
                    decision = enumOr(i.optString("decision"), Decision.REVIEW),
                    sourceUrl = i.optStr("sourceUrl"),
                    sizeBytes = if (i.isNull("size")) null else i.optLong("size"),
                    localFile = i.optStr("localFile"),
                    expectedSha256 = i.optStr("expectedSha256"),
                    computedSha256 = i.optStr("computedSha256"),
                    verification = i.optStr("verification")?.let { v -> Verification.values().firstOrNull { it.name == v } },
                    note = i.optStr("note")?.let { v -> ItemNote.values().firstOrNull { it.name == v } },
                    changed = i.optBoolean("changed", false),
                ),
            )
        }
        val cats = ArrayList<String>()
        o.optJSONArray("categories")?.let { a -> for (n in 0 until a.length()) cats.add(a.optString(n)) }
        return StoreDraft(
            id = o.getString("id"),
            repo = RepoRef(o.getString("owner"), o.getString("repo")),
            createdAt = o.optLong("createdAt"), updatedAt = o.optLong("updatedAt"),
            approved = o.optBoolean("approved"), approvedAt = o.optLong("approvedAt"),
            mode = enumOr(o.optString("mode"), ImportMode.INFO_ONLY),
            sizeKb = if (o.isNull("sizeKb")) null else o.optLong("sizeKb"),
            defaultBranch = o.optString("defaultBranch", "main"),
            displayName = o.optString("displayName", ""), displayDescription = o.optString("displayDescription", ""),
            categories = cats, coverItemId = o.optStr("cover"), items = items,
        )
    }
}
