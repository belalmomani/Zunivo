package com.zunivo.app.data.remote

import com.zunivo.app.domain.AssetInfo
import com.zunivo.app.domain.ReleaseInfo
import com.zunivo.app.domain.RepoInfo
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64

/** Pure parsing of GitHub REST responses (shapes from docs.github.com/rest). Throws JSONException when malformed. */
object GitHubParser {
    private fun JSONObject.str(key: String): String? =
        if (isNull(key)) null else optString(key, "").trim().ifEmpty { null }

    fun parseRepo(json: String): RepoInfo {
        val o = JSONObject(json)
        val license = o.optJSONObject("license")
        return RepoInfo(
            name = o.str("name") ?: throw org.json.JSONException("no name"),
            description = o.str("description"),
            ownerLogin = o.optJSONObject("owner")?.str("login") ?: throw org.json.JSONException("no owner"),
            htmlUrl = o.str("html_url") ?: throw org.json.JSONException("no url"),
            language = o.str("language"),
            licenseName = license?.str("name"),
            licenseSpdx = license?.str("spdx_id")?.takeIf { it != "NOASSERTION" },
            licenseUrl = license?.str("url"),
            sizeKb = o.optLong("size", 0L),
            defaultBranch = o.str("default_branch") ?: "main",
            homepage = o.str("homepage"),
            hasWiki = o.optBoolean("has_wiki", false),
            archived = o.optBoolean("archived", false),
            fork = o.optBoolean("fork", false),
        )
    }

    /** Works for /readme and for single-file /contents responses (base64 body). Null if not a base64 file. */
    fun parseFileText(json: String): String? {
        val o = JSONObject(json)
        if (o.optString("encoding") != "base64") return null
        val content = o.optString("content", "")
        return try {
            String(Base64.getMimeDecoder().decode(content), Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    fun parseReleases(json: String): List<ReleaseInfo> {
        val arr = JSONArray(json)
        val out = ArrayList<ReleaseInfo>()
        for (i in 0 until arr.length()) {
            val r = arr.optJSONObject(i) ?: continue
            if (r.optBoolean("draft", false)) continue
            val tag = r.str("tag_name") ?: continue
            val assets = ArrayList<AssetInfo>()
            val aa = r.optJSONArray("assets")
            if (aa != null) for (j in 0 until aa.length()) {
                val a = aa.optJSONObject(j) ?: continue
                val name = a.str("name") ?: continue
                val url = a.str("browser_download_url") ?: continue
                val digest = a.str("digest")?.lowercase()
                val sha = if (digest != null && digest.startsWith("sha256:") && digest.length == 71) digest.removePrefix("sha256:") else null
                assets.add(AssetInfo(name, a.optLong("size", 0L), url, sha))
            }
            out.add(ReleaseInfo(tag, r.str("name"), r.str("body"), r.str("html_url"), r.optBoolean("prerelease", false), r.str("published_at"), assets))
        }
        return out
    }
}
