package com.awan.feature.inventory.impl.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.feature.inventory.impl.presentation.InventoryAction
import com.awan.feature.inventory.impl.presentation.InventoryState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingCardEquipsItemAndInfoOpensDetailsSheet() {
        val customization = customization(isEquipped = false)
        var action: InventoryAction? = null

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        customizations = listOf(customization),
                        isLoading = false,
                    ),
                    onAction = { action = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-card-${customization.itemId}").performClick()
        assertEquals(InventoryAction.Equip(customization.itemId), action)

        composeRule.onNodeWithContentDescription("View details for ${customization.name}").performClick()
        composeRule.onNodeWithTag("inventory-details-artwork").assertIsDisplayed()
        composeRule.onNodeWithText(customization.description).assertIsDisplayed()
        composeRule.onNodeWithText("Equip").assertIsDisplayed()
    }

    @Test
    fun equippedItemShowsDisabledEquippedActionInDetailsSheet() {
        val customization = customization(isEquipped = true)

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        customizations = listOf(customization),
                        isLoading = false,
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("View details for ${customization.name}").performClick()
        composeRule.onNodeWithText("Equipped").assertIsNotEnabled()
    }

    private fun customization(isEquipped: Boolean) = OwnedCustomization(
        inventoryId = "inventory-frame",
        itemId = "frame",
        name = "Gold Frame",
        description = "A warm frame for milestone moments.",
        imageUrl = null,
        type = CustomizationType.FRAME,
        rarity = CustomizationRarity.RARE,
        acquiredAt = "2026-08-01T00:00:00Z",
        isEquipped = isEquipped,
    )
}
