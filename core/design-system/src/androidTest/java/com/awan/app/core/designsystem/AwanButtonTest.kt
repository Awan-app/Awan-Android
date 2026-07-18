package com.awan.app.core.designsystem

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class AwanButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun enabledButton_exposesSemanticsAndClicks() {
        var clicks = 0
        composeRule.setContent {
            AwanTheme {
                AwanButton(onClick = { clicks++ }) { AwanText("ADD TASK") }
            }
        }

        composeRule.onNodeWithText("ADD TASK")
            .assertIsEnabled()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()
        composeRule.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun disabledButton_doesNotClick() {
        var clicks = 0
        composeRule.setContent {
            AwanTheme {
                AwanButton(onClick = { clicks++ }, enabled = false) { AwanText("DISABLED") }
            }
        }

        composeRule.onNodeWithText("DISABLED")
            .assertIsNotEnabled()
            .performClick()
        composeRule.runOnIdle { assertEquals(0, clicks) }
    }

    @Test
    fun button_meetsMinimumTouchTarget() {
        composeRule.setContent {
            AwanTheme {
                AwanButton(onClick = {}) { AwanText("TARGET") }
            }
        }

        composeRule.onNodeWithText("TARGET").assertHeightIsAtLeast(48.dp)
    }
}
