package com.zunivo.app.ui

import com.zunivo.app.i18n.AppLanguage
import com.zunivo.app.ui.navigation.NavState
import com.zunivo.app.ui.navigation.SubScreen
import com.zunivo.app.ui.navigation.Tab
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavStateTest {
    @Test fun startsOnHomeWithNothingToGoBackTo() {
        val s = NavState()
        assertEquals(Tab.HOME, s.tab)
        assertFalse(s.canGoBack())
        assertEquals(s, s.back())
    }

    @Test fun backFromSubScreenReturnsToSameTab() {
        val s = NavState().selectTab(Tab.MORE).open(SubScreen.SETTINGS)
        assertTrue(s.canGoBack())
        val back = s.back()
        assertEquals(Tab.MORE, back.tab)
        assertNull(back.sub)
    }

    @Test fun backFromOtherTabGoesHome() {
        assertEquals(Tab.HOME, NavState().selectTab(Tab.FAVORITES).back().tab)
    }

    @Test fun selectingTabClosesSubScreen() {
        val s = NavState().open(SubScreen.ABOUT).selectTab(Tab.SEARCH)
        assertNull(s.sub)
        assertEquals(Tab.SEARCH, s.tab)
    }

    @Test fun languageTagsRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("fr"))
    }
}
