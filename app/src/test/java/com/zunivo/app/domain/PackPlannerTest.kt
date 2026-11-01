package com.zunivo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackPlannerTest {
    private fun entry(name: String, spdx: String?) =
        CollectionEntry(IconSnapshot(IconRef("s", name), "Set", "Auth", null, spdx, null), name)

    private fun pack(color: String?, vararg e: CollectionEntry) =
        IconCollection("p", CollectionKind.PACK, "P", "", e.toList(), color, 0, 0)

    @Test fun onlyRedistributableLicensesAreIncluded() {
        val plan = PackPlanner.plan(
            pack(null, entry("mit", "MIT"), entry("by", "CC-BY-4.0"), entry("nc", "CC-BY-NC-4.0"),
                entry("unk", null), entry("gpl", "GPL-3.0-only"), entry("nd", "CC-BY-ND-4.0")),
        )
        assertEquals(listOf("mit", "by"), plan.included.map { it.fileName })
        assertEquals(
            listOf(ExcludeReason.NON_COMMERCIAL, ExcludeReason.LICENSE_UNKNOWN, ExcludeReason.LICENSE_NOT_RECOGNIZED, ExcludeReason.NO_DERIVATIVES),
            plan.excluded.map { it.second },
        )
    }

    @Test fun docsListIncludedExcludedAndKeepLicenseInfo() {
        val c = pack("#6366F1", entry("mit", "MIT"), entry("unk", null))
        val plan = PackPlanner.plan(c)
        val readme = PackDocs.readme(c, plan, "2026-10-31 10:00:00 +0000", emptyList())
        assertTrue(readme.contains("svg/mit.svg"))
        assertTrue(readme.contains("s:unk: license unknown"))
        assertTrue(readme.contains("not legal advice"))
        assertTrue(PackDocs.licenses(plan).contains("License: MIT"))
        assertTrue(PackDocs.colorsJson(c).contains("\"#6366F1\""))
        assertTrue(PackDocs.colorsJson(c.copy(colorHex = null)).contains("null"))
    }

    @Test fun luminanceSeparatesLightFromDark() {
        assertTrue(HexColor.luminance("#FFFFFF") > 0.9)
        assertTrue(HexColor.luminance("#0B1020") < 0.1)
        assertEquals(0.0, HexColor.luminance("bad"), 0.0)
    }
}
