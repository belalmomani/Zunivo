package com.zunivo.app.feature.search

import com.zunivo.app.domain.IconItem

enum class IconStyle {
    OUTLINE, FILLED, UNKNOWN;

    companion object {
        private val outlineTokens = setOf("outline", "outlined", "line", "linear", "thin")
        private val filledTokens = setOf("fill", "filled", "solid", "bold")

        /** Name-based guess only (Iconify has no style field), so the UI labels this filter as approximate. */
        fun guess(iconName: String): IconStyle {
            val tokens = iconName.lowercase().split('-', '_', '.')
            return when {
                tokens.any { it in outlineTokens } -> OUTLINE
                tokens.any { it in filledTokens } -> FILLED
                else -> UNKNOWN
            }
        }
    }
}

enum class StyleFilter { ALL, OUTLINE, FILLED, COLORED }

fun List<IconItem>.applyStyle(mode: StyleFilter): List<IconItem> = when (mode) {
    StyleFilter.ALL -> this
    StyleFilter.OUTLINE -> filter { IconStyle.guess(it.ref.name) == IconStyle.OUTLINE }
    StyleFilter.FILLED -> filter { IconStyle.guess(it.ref.name) == IconStyle.FILLED }
    StyleFilter.COLORED -> filter { it.set?.palette == true }
}
