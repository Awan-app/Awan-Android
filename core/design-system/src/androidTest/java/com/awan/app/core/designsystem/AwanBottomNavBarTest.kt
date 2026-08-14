package com.awan.app.core.designsystem

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AwanBottomNavBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val homeItem = BottomNavItem(
        id = "HOME",
        selectedIcon = Icons.Rounded.Home,
        unselectedIcon = Icons.Outlined.Home,
        label = "Home",
    )
    private val goalsItem = BottomNavItem(
        id = "GOALS",
        selectedIcon = Icons.Rounded.EmojiEvents,
        unselectedIcon = Icons.Outlined.EmojiEvents,
        label = "Goals",
    )
    private val fabItem = BottomNavItem(
        id = "AI_ACTION",
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Rounded.AutoAwesome,
        label = "AI Action",
        isFab = true,
    )

    @Test
    fun navBarItemClick_triggersOnItemSelected() {
        var selectedItem: BottomNavItem? = null
        composeRule.setContent {
            AwanTheme {
                AwanBottomNavBar(
                    items = listOf(homeItem, goalsItem, fabItem),
                    selectedItemId = "HOME",
                    onItemSelected = { selectedItem = it },
                    onFabClick = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Goals")
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle {
            assertEquals(goalsItem, selectedItem)
        }
    }

    @Test
    fun fabClick_triggersOnFabClick() {
        var fabClicked = false
        composeRule.setContent {
            AwanTheme {
                AwanBottomNavBar(
                    items = listOf(homeItem, goalsItem, fabItem),
                    selectedItemId = "HOME",
                    onItemSelected = {},
                    onFabClick = { fabClicked = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("AI Action")
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle {
            assertTrue(fabClicked)
        }
    }
}
