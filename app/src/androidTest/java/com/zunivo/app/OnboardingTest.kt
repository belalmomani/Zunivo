package com.zunivo.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.zunivo.app.core.FirstRun
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

/** First launch: the three-step tour appears once, can be walked through, and is not shown again. */
class OnboardingTest {
    private val composeRule = createAndroidComposeRule<MainActivity>()
    private val ctx get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun s(id: Int): String = ctx.getString(id)

    @get:Rule
    val chain: RuleChain = RuleChain
        .outerRule(object : ExternalResource() {
            override fun before() { FirstRun.reset(InstrumentationRegistry.getInstrumentation().targetContext) }
            override fun after() { FirstRun.markDone(InstrumentationRegistry.getInstrumentation().targetContext) }
        })
        .around(composeRule)

    @Test fun tourShowsThreeStepsThenTheApp() {
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithText(s(R.string.onboarding_1_title)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(s(R.string.onboarding_1_title)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.onboarding_next)).performClick()
        composeRule.onNodeWithText(s(R.string.onboarding_2_title)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.onboarding_next)).performClick()
        composeRule.onNodeWithText(s(R.string.onboarding_3_title)).assertIsDisplayed()
        composeRule.onNodeWithText(s(R.string.onboarding_start)).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(s(R.string.nav_more)).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(s(R.string.nav_search)).assertIsDisplayed()
        check(FirstRun.isDone(ctx))
    }
}
