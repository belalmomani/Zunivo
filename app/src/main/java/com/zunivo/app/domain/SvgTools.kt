package com.zunivo.app.domain

/** Rejects SVG that could carry active or external content. Icons from Iconify are plain shapes. */
object SvgSafety {
    const val MAX_CHARS = 500_000
    private val onAttr = Regex("\\son[a-z]+\\s*=")
    private val banned = listOf(
        "<script", "<foreignobject", "javascript:", "<!entity", "<!doctype", "<iframe", "<embed",
        "<object", "data:text/html", "href=\"http", "href='http",
    )

    fun isSafe(raw: String): Boolean {
        if (raw.length > MAX_CHARS) return false
        val lower = raw.trim().lowercase()
        if (!(lower.startsWith("<svg") || lower.startsWith("<?xml"))) return false
        if (!lower.contains("<svg") || !lower.contains("</svg>")) return false
        if (banned.any { lower.contains(it) }) return false
        return !onAttr.containsMatchIn(lower)
    }
}

/** Wraps an Iconify SVG in a square canvas with padding and optional background, plus a provenance comment. */
object SvgComposer {
    private val viewBoxAttr = Regex("viewBox\\s*=\\s*\"([^\"]+)\"")
    private val viewBoxValue = Regex("^\\s*-?[\\d.]+[\\s,]+-?[\\d.]+[\\s,]+[\\d.]+[\\s,]+[\\d.]+\\s*$")

    fun compose(raw: String, sizePx: Int, paddingPct: Int, bgHex: String?, meta: String): String? {
        val start = raw.indexOf("<svg")
        if (start < 0) return null
        val tagEnd = raw.indexOf('>', start)
        val end = raw.lastIndexOf("</svg>")
        if (tagEnd < 0 || end < 0 || tagEnd >= end) return null
        val viewBox = viewBoxAttr.find(raw.substring(start, tagEnd))?.groupValues?.get(1) ?: return null
        if (!viewBoxValue.matches(viewBox)) return null
        val inner = raw.substring(tagEnd + 1, end)

        val size = sizePx.coerceIn(EditParams.MIN_SIZE, EditParams.MAX_SIZE)
        val pad = size * paddingPct.coerceIn(0, EditParams.MAX_PADDING) / 100
        val innerSize = size - 2 * pad
        if (innerSize <= 0) return null
        val bg = HexColor.normalize(bgHex)
        val comment = meta.replace("--", "- -").replace(Regex("[\\u0000-\\u001F]"), " ").trim()

        return buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
            append("<!-- ").append(comment).append(" -->\n")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
            append("width=\"$size\" height=\"$size\" viewBox=\"0 0 $size $size\">\n")
            if (bg != null) append("  <rect width=\"$size\" height=\"$size\" fill=\"$bg\"/>\n")
            append("  <svg x=\"$pad\" y=\"$pad\" width=\"$innerSize\" height=\"$innerSize\" viewBox=\"$viewBox\">")
            append(inner)
            append("</svg>\n</svg>\n")
        }
    }
}
