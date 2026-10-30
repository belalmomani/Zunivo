package com.zunivo.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AppInfoTest {
    @Test fun identityIsZunivo() {
        assertEquals("Zunivo", AppInfo.NAME_EN)
        assertEquals("زينوفو", AppInfo.NAME_AR)
        assertEquals("com.zunivo.app", AppInfo.PACKAGE_NAME)
    }

    @Test fun noLegacyNameInIdentity() {
        val legacy = listOf("ic", "ona").joinToString("")
        val all = listOf(AppInfo.NAME_EN, AppInfo.NAME_AR, AppInfo.PACKAGE_NAME, AppInfo.REPO_URL)
        assertFalse(all.any { it.contains(legacy, ignoreCase = true) })
    }
}
