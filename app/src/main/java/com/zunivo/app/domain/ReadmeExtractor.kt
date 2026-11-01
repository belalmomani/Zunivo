package com.zunivo.app.domain

import java.net.URI

object ReadmeExtractor {
    data class ImageRef(val url: String, val alt: String, val allowedHost: Boolean)

    private val md = Regex("""!\[([^\]]*)\]\(\s*<?([^)\s>]+)>?(?:\s+["'][^"']*["'])?\s*\)""")
    private val html = Regex("""<img\b[^>]*?\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val okExt = setOf("png", "jpg", "jpeg", "gif", "webp")
    private val badgeMarks = listOf("shields.io", "badge", "badgen", "travis-ci", "codecov", "coveralls", "/actions/workflows/")

    fun resolve(src: String, ref: RepoRef, branch: String): String? {
        val s = src.trim()
        if (s.isEmpty() || s.startsWith("data:") || s.startsWith("#")) return null
        if (s.startsWith("//")) return resolve("https:$s", ref, branch)
        if (s.startsWith("http://")) return null          // insecure
        if (s.startsWith("https://")) return s
        if (s.contains("://")) return null
        val path = s.removePrefix("./").trimStart('/')
        val segments = path.split('/')
        if (segments.any { it == ".." || it.isEmpty() }) return null
        return "https://raw.githubusercontent.com/${ref.owner}/${ref.repo}/$branch/" + path.replace(" ", "%20")
    }

    private fun extension(url: String): String =
        url.substringBefore('#').substringBefore('?').substringAfterLast('/').substringAfterLast('.', "").lowercase()

    /** Raster images referenced by the README (badges and SVG are skipped). Order of appearance, de-duplicated. */
    fun images(readme: String, ref: RepoRef, branch: String, max: Int = 12): List<ImageRef> {
        val found = ArrayList<Pair<Int, Pair<String, String>>>()   // position, (src, alt)
        md.findAll(readme).forEach { found.add(it.range.first to (it.groupValues[2] to it.groupValues[1])) }
        html.findAll(readme).forEach { found.add(it.range.first to (it.groupValues[1] to "")) }
        val out = LinkedHashMap<String, ImageRef>()
        for ((_, pair) in found.sortedBy { it.first }) {
            val url = resolve(pair.first, ref, branch) ?: continue
            val lower = url.lowercase()
            if (badgeMarks.any { lower.contains(it) }) continue
            val host = try { URI(url).host } catch (e: Exception) { null }
            val allowed = GitHubHosts.isAllowed(host)
            val ext = extension(url)
            val extOk = ext in okExt || (ext.isEmpty() && allowed)      // user-attachments links have no extension
            if (!extOk) continue
            if (url !in out) out[url] = ImageRef(url, pair.second.trim(), allowed)
            if (out.size >= max) break
        }
        return out.values.toList()
    }
}
