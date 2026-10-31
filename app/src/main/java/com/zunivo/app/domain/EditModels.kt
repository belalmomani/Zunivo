package com.zunivo.app.domain

import java.util.Locale

object HexColor {
    private val RE = Regex("^#[0-9A-Fa-f]{6}$")
    fun isValid(s: String?): Boolean = s != null && RE.matches(s)

    /** "abc123", "#ABC123" -> "#ABC123"; null when not a 6-digit hex colour. */
    fun normalize(s: String?): String? {
        val t = s?.trim().orEmpty().let { if (it.startsWith("#")) it else "#$it" }
        return if (RE.matches(t)) t.uppercase(Locale.ROOT) else null
    }

    fun fromArgb(argb: Int): String = String.format(Locale.ROOT, "#%06X", argb and 0xFFFFFF)
}

/** Everything the editor can change. The original icon is never modified; these are applied to a copy. */
data class EditParams(
    val colorHex: String? = null,   // null = the icon's own colours
    val bgHex: String? = null,      // null = transparent background
    val sizePx: Int = 256,
    val paddingPct: Int = 10,
    val rotation: Int = 0,          // 0, 90, 180, 270
    val flipH: Boolean = false,
    val flipV: Boolean = false,
) {
    fun isDefault(): Boolean = this == EditParams()

    fun sanitized(): EditParams = copy(
        colorHex = HexColor.normalize(colorHex),
        bgHex = HexColor.normalize(bgHex),
        sizePx = sizePx.coerceIn(MIN_SIZE, MAX_SIZE),
        paddingPct = paddingPct.coerceIn(0, MAX_PADDING),
        rotation = (((rotation % 360) + 360) % 360) / 90 * 90,
    )

    /** Iconify applies colour / rotate / flip itself (documented in docs/api/svg.md). */
    fun svgUrl(ref: IconRef, height: Int? = null): String {
        val query = buildList {
            if (height != null) add("height=$height")
            if (colorHex != null) add("color=" + colorHex.replace("#", "%23"))
            val turns = (rotation / 90) % 4
            if (turns != 0) add("rotate=$turns")
            val flips = buildList {
                if (flipH) add("horizontal")
                if (flipV) add("vertical")
            }
            if (flips.isNotEmpty()) add("flip=" + flips.joinToString(","))
        }
        val suffix = if (query.isEmpty()) "" else "?" + query.joinToString("&")
        return "${IconifyEndpoints.BASE_URL}/${ref.prefix}/${ref.name}.svg$suffix"
    }

    companion object {
        const val MIN_SIZE = 32
        const val MAX_SIZE = 1024
        const val MAX_PADDING = 40
    }
}

/** A saved edit: params + a snapshot of the original's source/licence so it is never lost or re-attributed. */
data class IconEdit(
    val id: String,
    val icon: IconRef,
    val setName: String?,
    val authorName: String?,
    val licenseTitle: String?,
    val licenseSpdx: String?,
    val licenseUrl: String?,
    val params: EditParams,
    val createdAt: Long,
    val updatedAt: Long,
)
