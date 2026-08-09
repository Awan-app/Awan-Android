package com.awan.app.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AwanUserAvatarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun frameOverlay_isRenderedOverTheAccessibleAvatar() {
        composeRule.setContent {
            AwanTheme {
                AwanUserAvatar(
                    profilePictureUrl = "",
                    frameImageUrl = "",
                    isDark = false,
                    contentDescription = "User avatar",
                    modifier = Modifier.size(80.dp),
                )
            }
        }

        composeRule.onNodeWithContentDescription("User avatar").assertExists()
        composeRule.onNodeWithTag(AwanUserAvatarFrameTestTag, useUnmergedTree = true)
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.TestTag))
    }
}
