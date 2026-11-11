package com.zunivo.app

import android.content.Context
import android.content.res.Configuration
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNode
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import com.zunivo.app.i18n.AppLanguage
import com.zunivo.app.i18n.LocaleManager
import com.zunivo.app.core.FirstRun
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Test
import org.junit.runners.MethodSorters
import java.util.Locale

/**
 * On-device smoke tests. They need no network: they drive navigation, settings, the editor's empty state,
 * the store's local URL validation and the privacy screen. Strings come from resources so the tests
 * do not depend on the emulator's language (it must be English or the app's saved language must be the system one).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SmokeTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val rule get() = composeRule

    /** The welcome tour is marked as seen before the activity starts, so every test begins at the app itself. */
    @get:Rule
    val chain: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { FirstRun.markDone(InstrumentationRegistry.getInstrumentation().targetContext) }
        })
        .around(composeRule)

    private val ctx: Context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun s(id: Int): String = ctx.getString(id)

    private fun waitForHome() {
        rule.waitUntil(timeoutMillis = 10_000) { rule.onAllNodesWithText(s(R.string.nav_more)).fetchSemanticsNodes().isNotEmpty() }
    }

    /** Opens More and scrolls the list to the entry (the list is lazy, so it may not be composed yet). */
    private fun openFromMore(titleRes: Int) {
        waitForHome()
        rule.onNodeWithText(s(R.string.nav_more)).performClick()
        rule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(s(titleRes)))
        rule.onNodeWithText(s(titleRes)).performClick()
    }

    @Test fun a_bottomNavigationIsVisibleAfterSplash() {
        waitForHome()
        listOf(R.string.nav_home, R.string.nav_search, R.string.nav_favorites, R.string.nav_more).forEach {
            rule.onNodeWithText(s(it)).assertIsDisplayed()
        }
    }

    @Test fun b_searchTabShowsFieldAndCategories() {
        waitForHome()
        rule.onNodeWithText(s(R.string.nav_search)).performClick()
        rule.onNodeWithText(s(R.string.search_idle_title)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.cat_tech)).assertIsDisplayed()
    }

    @Test fun c_favoritesStartsEmpty() {
        waitForHome()
        rule.onNodeWithText(s(R.string.nav_favorites)).performClick()
        rule.onNodeWithText(s(R.string.fav_empty_title)).assertIsDisplayed()
    }

    @Test fun d_editorWithoutIconShowsGuidance() {
        openFromMore(R.string.menu_editor)
        rule.onNodeWithText(s(R.string.editor_go_search)).assertIsDisplayed()
    }

    @Test fun e_storeRejectsAnInvalidLinkWithoutNetwork() {
        openFromMore(R.string.menu_store)
        rule.onNode(hasSetTextAction()).performTextInput("not a github link")
        rule.onNodeWithText(s(R.string.store_check)).performClick()
        rule.onNodeWithText(s(R.string.store_err_invalid)).assertIsDisplayed()
    }

    @Test fun f_privacyScreenListsWhatIsSent() {
        openFromMore(R.string.menu_privacy)
        rule.onNodeWithText(s(R.string.privacy_s2_title)).assertIsDisplayed()
    }

    @Test fun g_settingsOffersLanguageAndStorage() {
        openFromMore(R.string.menu_settings)
        rule.onNodeWithText(s(R.string.settings_language)).assertIsDisplayed()
        rule.onNodeWithText(s(R.string.menu_storage)).performScrollTo().performClick()
        rule.onNodeWithText(s(R.string.storage_clear_cache)).assertIsDisplayed()
    }

    @Test fun h_designersAreHonestWhenNotConfigured() {
        if (com.zunivo.app.data.remote.SupabaseConfig.enabled) return   // configured builds show the real screen instead
        openFromMore(R.string.menu_designers)
        rule.onNodeWithText(s(R.string.designers_not_connected_title)).assertIsDisplayed()
    }

    /** Runs last (name order). Switches to Arabic, checks the labels changed, then restores the system language. */
    @Test fun z_switchingToArabicChangesLabelsAndIsRestored() {
        val arabic = ctx.createConfigurationContext(Configuration(ctx.resources.configuration).apply { setLocale(Locale("ar")) })
        try {
            openFromMore(R.string.menu_settings)
            rule.onNodeWithText(s(R.string.lang_arabic)).performClick()
            rule.waitUntil(timeoutMillis = 10_000) {
                rule.onAllNodesWithText(arabic.getString(R.string.settings_language)).fetchSemanticsNodes().isNotEmpty()
            }
        } finally {
            LocaleManager.save(ctx, AppLanguage.SYSTEM)   // never leave the device in Arabic for other runs
        }
    }
}
