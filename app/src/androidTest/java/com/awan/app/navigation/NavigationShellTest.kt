package com.awan.app.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.awan.app.MainActivity
import com.awan.app.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationShellTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun tabsAreVisibleAndKeepExistingRouteBehavior() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val home = context.getString(R.string.navigation_home)
        val arena = context.getString(R.string.navigation_arena)
        val calendar = context.getString(R.string.navigation_calendar)
        val settings = context.getString(R.string.navigation_settings)

        composeRule.onNodeWithText(home).assertIsDisplayed().assertIsSelected()
        composeRule.onNodeWithText(arena).assertIsDisplayed()
        composeRule.onNodeWithText(calendar).assertIsDisplayed()
        composeRule.onNodeWithText(settings).assertIsDisplayed()

        composeRule.onNodeWithText(arena).performClick().assertIsSelected()
        composeRule.onNodeWithText("Arena Screen").assertIsDisplayed()

        composeRule.onNodeWithText(home).performClick().assertIsSelected()
        composeRule.onNodeWithText("Home Screen").assertIsDisplayed()
    }
}
