package com.zunivo.app.domain

/** One-line source/licence note embedded in every exported SVG so the information is never lost. */
object Provenance {
    fun forIcon(s: IconSnapshot, recolored: Boolean): String {
        val license = listOfNotNull(s.licenseTitle, s.licenseSpdx).distinct().joinToString(" / ")
        return "Source: Iconify (api.iconify.design); icon ${s.icon.id}; set ${s.setName ?: s.icon.prefix}; " +
            "original author: ${s.authorName ?: "unknown"}; " +
            "license: ${license.ifBlank { "unknown - verify before redistribution" }}" +
            (s.licenseUrl?.let { " ($it)" } ?: "") +
            if (recolored) "; recolored by the pack owner with Zunivo - the original author did not make this change." else "."
    }
}
