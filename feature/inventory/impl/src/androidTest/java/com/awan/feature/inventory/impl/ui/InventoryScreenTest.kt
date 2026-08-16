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
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.inventory.impl.presentation.InventoryAction
import com.awan.feature.inventory.impl.presentation.InventoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InventoryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tappingCardEquipsItemAndInfoOpensDetailsSheet() {
        val ownedItem = item()
        var action: InventoryAction? = null

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        items = listOf(ownedItem),
                        equippedItemIds = emptySet(),
                        isLoading = false,
                    ),
                    onAction = { action = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-card-${ownedItem.item.id}").performClick()
        assertEquals(InventoryAction.Equip(ownedItem.item.id), action)

        composeRule.onNodeWithContentDescription("View details for ${ownedItem.item.name}").performClick()
        assertEquals(InventoryAction.OpenDetails(ownedItem.item.id), action)
    }

    @Test
    fun detailsSheetDisplaysItemInfoAndEquipAction() {
        val ownedItem = item()
        var action: InventoryAction? = null

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        items = listOf(ownedItem),
                        equippedItemIds = emptySet(),
                        detailsItemId = ownedItem.item.id,
                        isLoading = false,
                    ),
                    onAction = { action = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-details-artwork").assertIsDisplayed()
        composeRule.onNodeWithText(ownedItem.item.description).assertIsDisplayed()
        composeRule.onNodeWithText("Pick").assertIsDisplayed().performClick()
        assertEquals(InventoryAction.Equip(ownedItem.item.id), action)
    }

    @Test
    fun equippedItemShowsDisabledEquippedActionInDetailsSheet() {
        val ownedItem = item()

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        items = listOf(ownedItem),
                        equippedItemIds = setOf(ownedItem.item.id),
                        detailsItemId = ownedItem.item.id,
                        isLoading = false,
                    ),
                    onAction = {},
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-details-artwork").assertIsDisplayed()
        composeRule.onNodeWithText("Already Equipped").assertIsNotEnabled()
    }

    @Test
    fun tappingDefaultCardWhenEquippedDispatchesUnequipAction() {
        val ownedItem = item()
        var action: InventoryAction? = null

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        items = listOf(ownedItem),
                        equippedItemIds = setOf(ownedItem.item.id),
                        isLoading = false,
                    ),
                    onAction = { action = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-card-default-frame").performClick()
        assertEquals(InventoryAction.Unequip(StoreItemType.FRAME), action)
    }

    @Test
    fun tappingDefaultCardWhenAlreadyDefaultDoesNotDispatchUnequipAction() {
        val ownedItem = item()
        var action: InventoryAction? = null

        composeRule.setContent {
            AwanTheme {
                InventoryScreen(
                    state = InventoryState(
                        items = listOf(ownedItem),
                        equippedItemIds = emptySet(),
                        isLoading = false,
                    ),
                    onAction = { action = it },
                    onBack = {},
                )
            }
        }

        composeRule.onNodeWithTag("inventory-card-default-frame").performClick()
        assertNull(action)
    }

    private fun item() = OwnedItem(
        id = "inventory-frame",
        item = StoreItem(
            id = "frame",
            name = "Gold Frame",
            description = "A warm frame for milestone moments.",
            image = "",
            info = "rarity: rare",
            price = 500,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-08-01T00:00:00Z",
    )
}
