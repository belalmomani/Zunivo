package com.zunivo.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LicensePolicyTest {
    @Test fun missingLicenseIsUnknownAndBlocksDistribution() {
        val a = LicensePolicy.assess(null)
        assertEquals(LicenseKind.UNKNOWN, a.kind)
        assertFalse(a.autoDistributionOk)
        assertNull(a.commercialUseOk)
        assertEquals(LicenseKind.UNKNOWN, LicensePolicy.assess("  ").kind)
    }

    @Test fun mitIsPermissiveButKeepsNotice() {
        val a = LicensePolicy.assess("MIT")
        assertEquals(LicenseKind.PERMISSIVE, a.kind)
        assertTrue(a.noticeRequired)
        assertTrue(a.autoDistributionOk)
    }

    @Test fun cc0NeedsNoNotice() {
        assertFalse(LicensePolicy.assess("CC0-1.0").noticeRequired)
    }

    @Test fun ccByRequiresAttribution() {
        val a = LicensePolicy.assess("CC-BY-4.0")
        assertEquals(LicenseKind.ATTRIBUTION, a.kind)
        assertTrue(a.noticeRequired)
        assertEquals(true, a.commercialUseOk)
    }

    @Test fun shareAlikeDetected() {
        assertEquals(LicenseKind.SHARE_ALIKE, LicensePolicy.assess("CC-BY-SA-4.0").kind)
    }

    @Test fun nonCommercialBlocksCommercialAndAutoDistribution() {
        val a = LicensePolicy.assess("cc-by-nc-4.0")
        assertEquals(LicenseKind.NON_COMMERCIAL, a.kind)
        assertEquals(false, a.commercialUseOk)
        assertFalse(a.autoDistributionOk)
    }

    @Test fun noDerivativesBlocksModification() {
        val a = LicensePolicy.assess("CC-BY-ND-4.0")
        assertEquals(LicenseKind.NO_DERIVATIVES, a.kind)
        assertEquals(false, a.modificationOk)
    }

    @Test fun unrecognizedLicenseNeedsReview() {
        val a = LicensePolicy.assess("GPL-3.0-only")
        assertEquals(LicenseKind.REVIEW, a.kind)
        assertFalse(a.autoDistributionOk)
    }
}
