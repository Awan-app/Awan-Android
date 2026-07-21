package com.awan.app.core.designsystem

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** TEMPORARY: reproduces the foundation 1.11.4 inherited-style bug directly, to prove the diagnosis. */
@RunWith(AndroidJUnit4::class)
class TempLibraryBugProofTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val Big = TextStyle(fontFamily = FontFamily.Serif, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold)
    private val Small = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Light)

    // Matches the real design system: ONE shared Style instance per appearance, reused by every
    // node in the app. StyleElement.equals then returns true, so the node's update() never runs.
    private object SharedStyles {
        val big = Style {
            textStyle(TextStyle(fontFamily = FontFamily.Serif, fontSize = 40.sp, fontWeight = FontWeight.ExtraBold))
        }
        val small = Style {
            textStyle(TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Light))
        }
    }

    @Test
    fun sharedStyleSingletons_survivesReorder() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        val items = mutableStateListOf("Study", "Work", "Play")
        composeRule.setContent {
            Column(Modifier.styleable(null, SharedStyles.small)) {
                BasicText("YOUR DAY", modifier = Modifier.styleable(null, SharedStyles.small))
                items.forEach { name ->
                    key(name) {
                        Column(Modifier.styleable(null, SharedStyles.big)) {
                            BasicText(name, modifier = Modifier.styleable(null, SharedStyles.big))
                        }
                    }
                }
            }
        }

        val before = composeRule.onNodeWithText("Play").renderedStyle()
        composeRule.runOnIdle { items.add(0, items.removeAt(2)) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { items.add(2, items.removeAt(0)) }
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("Play").renderedStyle()

        assertEquals("shared-singleton size drifted", before.fontSize, after.fontSize)
        assertEquals("shared-singleton family drifted", before.fontFamily, after.fontFamily)
    }

    @Test
    fun sharedStyleSingletons_survivesContentChange() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        var clock by mutableStateOf("7:00 AM")
        composeRule.setContent {
            Column(Modifier.styleable(null, SharedStyles.small)) {
                BasicText("Starts", modifier = Modifier.styleable(null, SharedStyles.small))
                BasicText(clock, modifier = Modifier.styleable(null, SharedStyles.big))
            }
        }

        val before = composeRule.onNodeWithText("7:00 AM").renderedStyle()
        repeat(3) {
            clock = if (it % 2 == 0) "5:20 PM" else "7:00 AM"
            composeRule.waitForIdle()
        }
        val after = composeRule.onNodeWithText("5:20 PM").renderedStyle()

        assertEquals("shared-singleton size drifted", before.fontSize, after.fontSize)
        assertEquals("shared-singleton family drifted", before.fontFamily, after.fontFamily)
    }

    private fun SemanticsNodeInteraction.renderedStyle(): TextStyle {
        val results = mutableListOf<TextLayoutResult>()
        fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        return results.first().layoutInput.style
    }

    // The exact old mechanism: singleton Styles whose lambdas read LocalAwanTheme (so the resolve
    // happens under observeReads), real Baloo2/Nunito resources, applied via styleable on the text.
    private object OldStyles {
        val heading = Style {
            textStyle(LocalAwanTheme.currentValue.typography.heading)
            contentColor(LocalAwanTheme.currentValue.colors.textPrimary)
        }
        val meta = Style {
            textStyle(LocalAwanTheme.currentValue.typography.caption)
            contentColor(LocalAwanTheme.currentValue.colors.meta)
        }
        val title = Style {
            textStyle(LocalAwanTheme.currentValue.typography.title)
            contentColor(LocalAwanTheme.currentValue.colors.textPrimary)
        }
    }

    @Composable
    private fun OldAwanText(text: String, style: Style, modifier: Modifier = Modifier) {
        BasicText(text = text, modifier = modifier.styleable(null, style))
    }

    @Test
    fun oldMechanism_survivesContentChange() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        var clock by mutableStateOf("7:00 AM")
        composeRule.setContent {
            AwanTheme {
                Column(Modifier.styleable(null, AwanTheme.styles.screen)) {
                    OldAwanText("Starts", OldStyles.meta)
                    OldAwanText(clock, OldStyles.heading)
                }
            }
        }

        val before = composeRule.onNodeWithText("7:00 AM").renderedStyle()
        repeat(3) {
            clock = if (it % 2 == 0) "5:20 PM" else "7:00 AM"
            composeRule.waitForIdle()
        }
        val after = composeRule.onNodeWithText("5:20 PM").renderedStyle()

        assertEquals("old mechanism size drifted", before.fontSize, after.fontSize)
        assertEquals("old mechanism family drifted", before.fontFamily, after.fontFamily)
    }

    @Test
    fun oldMechanism_survivesReorder() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        val items = mutableStateListOf("Study", "Work", "Play")
        composeRule.setContent {
            AwanTheme {
                Column(Modifier.styleable(null, AwanTheme.styles.screen)) {
                    OldAwanText("YOUR DAY", OldStyles.meta)
                    items.forEach { name ->
                        key(name) {
                            Column(Modifier.styleable(null, AwanTheme.styles.surface)) {
                                OldAwanText(name, OldStyles.title)
                                OldAwanText("9:00 AM", OldStyles.meta)
                            }
                        }
                    }
                }
            }
        }

        val before = composeRule.onNodeWithText("Play").renderedStyle()
        composeRule.runOnIdle { items.add(0, items.removeAt(2)) }
        composeRule.waitForIdle()
        composeRule.runOnIdle { items.add(2, items.removeAt(0)) }
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("Play").renderedStyle()

        assertEquals("old mechanism size drifted after reorder", before.fontSize, after.fontSize)
        assertEquals("old mechanism family drifted after reorder", before.fontFamily, after.fontFamily)
    }

    @Test
    fun inheritedStyle_survivesContentChange() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        var clock by mutableStateOf("7:00 AM")
        composeRule.setContent {
            Column(Modifier.styleable(null, Style { textStyle(Small) })) {
                BasicText("Starts", modifier = Modifier.styleable(null, Style { textStyle(Small) }))
                BasicText(clock, modifier = Modifier.styleable(null, Style { textStyle(Big) }))
            }
        }

        val before = composeRule.onNodeWithText("7:00 AM").renderedStyle()
        clock = "5:20 PM"
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("5:20 PM").renderedStyle()

        assertEquals("font size drifted", before.fontSize, after.fontSize)
        assertEquals("font family drifted", before.fontFamily, after.fontFamily)
        assertEquals("font weight drifted", before.fontWeight, after.fontWeight)
    }

    @Test
    fun inheritedStyle_survivesReorder() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        val items = mutableStateListOf("Study", "Work", "Play")
        composeRule.setContent {
            Column {
                BasicText("YOUR DAY", modifier = Modifier.styleable(null, Style { textStyle(Small) }))
                items.forEach { name ->
                    key(name) {
                        Column(Modifier.styleable(null, Style { textStyle(Big) })) {
                            BasicText(name, modifier = Modifier.styleable(null, Style { textStyle(Big) }))
                        }
                    }
                }
            }
        }

        val before = composeRule.onNodeWithText("Play").renderedStyle()
        composeRule.runOnIdle { items.add(0, items.removeAt(2)) }
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("Play").renderedStyle()

        assertEquals("font size drifted after reorder", before.fontSize, after.fontSize)
        assertEquals("font family drifted after reorder", before.fontFamily, after.fontFamily)
        assertEquals("font weight drifted after reorder", before.fontWeight, after.fontWeight)
    }

    @Test
    fun inheritedStyle_survivesMovingBetweenStyledParents() {
        ComposeFoundationFlags.isInheritedTextStyleEnabled = true
        var inSmallParent by mutableStateOf(false)
        composeRule.setContent {
            val label = movableContentOf {
                BasicText("Play", modifier = Modifier.styleable(null, Style { textStyle(Big) }))
            }
            Column {
                Column(Modifier.styleable(null, Style { textStyle(Small) })) {
                    if (inSmallParent) label()
                }
                Column(Modifier.styleable(null, Style { textStyle(Big) })) {
                    if (!inSmallParent) label()
                }
            }
        }

        val before = composeRule.onNodeWithText("Play").renderedStyle()
        inSmallParent = true
        composeRule.waitForIdle()
        val after = composeRule.onNodeWithText("Play").renderedStyle()

        assertEquals("font size drifted after move", before.fontSize, after.fontSize)
        assertEquals("font family drifted after move", before.fontFamily, after.fontFamily)
    }
}
