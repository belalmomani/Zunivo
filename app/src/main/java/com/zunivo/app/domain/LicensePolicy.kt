package com.zunivo.app.domain

enum class LicenseKind { PERMISSIVE, ATTRIBUTION, SHARE_ALIKE, NON_COMMERCIAL, NO_DERIVATIVES, REVIEW, UNKNOWN }

/**
 * What the app may offer for an icon, derived from the set's SPDX id.
 * `null` means "not known" and is treated as NOT allowed wherever it gates an action.
 * This is a safety helper, not legal advice.
 */
data class LicenseAssessment(
    val kind: LicenseKind,
    val noticeRequired: Boolean,
    val commercialUseOk: Boolean?,
    val modificationOk: Boolean?,
    val autoDistributionOk: Boolean,
)

object LicensePolicy {
    private val NO_NOTICE = setOf("CC0-1.0", "UNLICENSE", "0BSD")
    private val PERMISSIVE = NO_NOTICE + setOf("MIT", "ISC", "APACHE-2.0", "BSD-2-CLAUSE", "BSD-3-CLAUSE", "ZLIB")

    val UNKNOWN = LicenseAssessment(LicenseKind.UNKNOWN, true, null, null, false)

    fun assess(spdx: String?): LicenseAssessment {
        val key = spdx?.trim()?.uppercase().orEmpty()
        if (key.isEmpty()) return UNKNOWN
        return when {
            key.startsWith("CC-BY-NC") -> LicenseAssessment(LicenseKind.NON_COMMERCIAL, true, false, !key.contains("-ND"), false)
            key.startsWith("CC-BY-ND") -> LicenseAssessment(LicenseKind.NO_DERIVATIVES, true, true, false, false)
            key.startsWith("CC-BY-SA") -> LicenseAssessment(LicenseKind.SHARE_ALIKE, true, true, true, true)
            key.startsWith("CC-BY") -> LicenseAssessment(LicenseKind.ATTRIBUTION, true, true, true, true)
            key in PERMISSIVE -> LicenseAssessment(LicenseKind.PERMISSIVE, key !in NO_NOTICE, true, true, true)
            else -> LicenseAssessment(LicenseKind.REVIEW, true, null, null, false)
        }
    }
}
