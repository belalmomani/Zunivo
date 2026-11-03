package com.zunivo.app.data.remote

import com.zunivo.app.domain.ReleaseUpdate
import com.zunivo.app.domain.VersionCompare
import org.json.JSONObject

/** Parses GitHub's `releases/latest` response. Returns null when the release has no usable APK. */
object UpdateParser {
    fun parseLatest(json: String): ReleaseUpdate? {
        val o = JSONObject(json)
        if (o.optBoolean("draft", false) || o.optBoolean("prerelease", false)) return null
        val tag = o.optString("tag_name", "").trim().ifEmpty { return null }
        val assets = o.optJSONArray("assets") ?: return null

        data class A(val name: String, val url: String, val size: Long, val digest: String?)
        val list = ArrayList<A>()
        for (i in 0 until assets.length()) {
            val a = assets.optJSONObject(i) ?: continue
            val name = a.optString("name", "")
            val url = a.optString("browser_download_url", "")
            if (name.isEmpty() || url.isEmpty()) continue
            val d = if (a.isNull("digest")) null else a.optString("digest", "").lowercase()
            list.add(A(name, url, a.optLong("size", 0L), d))
        }
        val apks = list.filter { it.name.endsWith(".apk", ignoreCase = true) && it.size > 0 }
        val apk = apks.firstOrNull { it.name.contains("release", ignoreCase = true) } ?: apks.firstOrNull() ?: return null
        val sha = apk.digest?.takeIf { it.startsWith("sha256:") && it.length == 71 }?.removePrefix("sha256:")
        val checksum = list.firstOrNull { it.name.equals(apk.name + ".sha256", ignoreCase = true) }?.url
        return ReleaseUpdate(
            tag = tag, version = VersionCompare.clean(tag),
            notes = o.optString("body", "").trim().ifEmpty { null },
            htmlUrl = o.optString("html_url", "").ifEmpty { null },
            apkName = apk.name, apkUrl = apk.url, apkSize = apk.size, sha256 = sha, checksumUrl = checksum,
        )
    }
}
