package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text appearance must survive the node being re-laid-out or moved. Routing typography through the
 * Styles API's inherited-text-style path did not: foundation 1.11.4 appends to
 * `StyleOuterNode.ancestorNodes` on every resolve and never clears it, so a text node that changed
 * its content or moved picked up the typography of a sibling that was no longer its ancestor.
 */
@RunWith(AndroidJUnit4::class)
class AwanTextStyleTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun SemanticsNodeInteraction.renderedStyle(): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style
    }

    private fun assertSameFace(expected: TextStyle, actual: TextStyle) {
        assertEquals(expected.fontFamily, actual.fontFamily)
        assertEquals(expected.fontSize, actual.fontSize)
        assertEquals(expected.fontWeight, actual.fontWeight)
    }

    @Test
    fun changingContent_keepsItsOwnFace_notASiblings() {
        var clock by mutableStateOf("7:00 AM")
        composeRule.setContent {
            AwanTheme {
                Column {
                    // A sibling in a different family (Nunito caption) — the style that used to leak.
                    AwanText("Starts", style = AwanTheme.styles.metaText)
                    AwanText(clock, style = AwanTheme.styles.clockText)
                }
            }
        }

        val before = composeRule.onNodeWithText("7:00 AM").renderedStyle()
        clock = "5:20 PM"
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("5:20 PM").renderedStyle()

        assertSameFace(before, after)
        assertEquals(AwanTypographyTokens.heading.fontFamily, after.fontFamily)
        assertEquals(AwanTypographyTokens.heading.fontSize, after.fontSize)
    }

    @Test
    fun reordering_keepsEachItemsFace() {
        val items = mutableStateListOf("Study", "Work", "Play")
        composeRule.setContent {
            AwanTheme {
                Column {
                    AwanText("YOUR DAY", style = AwanTheme.styles.metaText)
                    items.forEach { name -> key(name) { AwanText(name, style = AwanTheme.styles.titleText) } }
                }
            }
        }

        val before = composeRule.onNodeWithText("Play").renderedStyle()
        composeRule.runOnIdle { items.add(0, items.removeAt(2)) }
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("Play").renderedStyle()

        assertSameFace(before, after)
        assertEquals(AwanTypographyTokens.title.fontSize, after.fontSize)
    }

    @Test
    fun buttonLabel_inheritsButtonFaceThroughCompositionLocal() {
        composeRule.setContent {
            AwanTheme {
                AwanButton(onClick = {}) { AwanText("CONTINUE") }
            }
        }

        val style = composeRule.onNodeWithText("CONTINUE").renderedStyle()
        assertEquals(AwanTypographyTokens.button.fontFamily, style.fontFamily)
        assertEquals(AwanTypographyTokens.button.fontSize, style.fontSize)
    }

    @Test
    fun headerGreeting_usesThemeTextColorInLightAndDarkModes() {
        composeRule.setContent {
            AwanTheme(dark = false) {
                AwanHeaderBar(userName = "Awan", streakCount = 0)
            }
        }
        assertEquals(LightAwanColors.textPrimary, composeRule.onNodeWithText("Good afternoon,").renderedStyle().color)

        composeRule.setContent {
            AwanTheme(dark = true) {
                AwanHeaderBar(userName = "Awan", streakCount = 0)
            }
        }
        assertEquals(DarkAwanColors.textPrimary, composeRule.onNodeWithText("Good afternoon,").renderedStyle().color)
    }
}
