package com.zunivo.app.domain

object IconifyEndpoints {
    const val BASE_URL = "https://api.iconify.design"
}

/** An icon identified the way Iconify does: set prefix + name. */
data class IconRef(val prefix: String, val name: String) {
    val id: String get() = "$prefix:$name"

    /** Iconify renders SVG on demand. `color` only affects monotone icons (see Iconify docs). */
    fun svgUrl(height: Int? = null, colorHex: String? = null): String {
        val params = buildList {
            if (height != null) add("height=$height")
            if (colorHex != null) add("color=" + colorHex.replace("#", "%23"))
        }
        val query = if (params.isEmpty()) "" else "?" + params.joinToString("&")
        return "${IconifyEndpoints.BASE_URL}/$prefix/$name.svg$query"
    }

    companion object {
        private val SAFE = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]*$")

        /** Parses "prefix:name"; returns null for anything that is not a safe identifier. */
        fun parse(id: String?): IconRef? {
            if (id == null) return null
            val idx = id.indexOf(':')
            if (idx <= 0 || idx == id.length - 1) return null
            val prefix = id.substring(0, idx)
            val name = id.substring(idx + 1)
            return if (SAFE.matches(prefix) && SAFE.matches(name)) IconRef(prefix, name) else null
        }
    }
}

/** Icon-set metadata as reported by Iconify. Any field can be missing; never invent values. */
data class IconSetInfo(
    val prefix: String,
    val name: String,
    val authorName: String?,
    val authorUrl: String?,
    val licenseTitle: String?,
    val licenseSpdx: String?,
    val licenseUrl: String?,
    val palette: Boolean?,
)

data class IconItem(val ref: IconRef, val set: IconSetInfo?)

/** One parsed `/search` response. */
data class SearchPage(
    val icons: List<IconRef>,
    val total: Int,
    val limit: Int,
    val sets: Map<String, IconSetInfo>,
)
