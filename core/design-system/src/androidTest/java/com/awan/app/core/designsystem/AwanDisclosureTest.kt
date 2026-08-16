package com.awan.app.core.designsystem

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AwanDisclosureTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun disclosureTogglesSurfaceAndStateDescription() {
        var expanded by mutableStateOf(false)

        composeRule.setContent {
            AwanTheme {
                AwanDisclosure(
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    label = "ADD IMAGE",
                    stateDescription = if (expanded) "Expanded" else "Collapsed",
                ) {
                    AwanText("Camera")
                    AwanText("Gallery")
                }
            }
        }

        composeRule.onAllNodesWithText("Camera").assertCountEquals(0)
        val trigger = composeRule.onNodeWithText("ADD IMAGE")
        trigger.assertHeightIsAtLeast(48.dp)
        trigger
            .assertHasClickAction()
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Collapsed",
                ),
            )
            .performClick()

        composeRule.runOnIdle { assertTrue(expanded) }
        composeRule.onNodeWithText("Camera").assertIsDisplayed()
        composeRule.onNodeWithText("Gallery").assertIsDisplayed()
        composeRule.onNodeWithText("ADD IMAGE").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Expanded"),
        )

        composeRule.onNodeWithText("ADD IMAGE").performClick()
        composeRule.onAllNodesWithText("Camera").assertCountEquals(0)
        composeRule.onAllNodesWithText("Gallery").assertCountEquals(0)
    }
}
