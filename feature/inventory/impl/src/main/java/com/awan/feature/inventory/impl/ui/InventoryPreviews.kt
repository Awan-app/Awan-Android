package com.awan.feature.inventory.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.feature.inventory.impl.presentation.InventoryState

private val previewInventory = listOf(
    OwnedItem(
        id = "inventory-gold-frame",
        item = StoreItem(
            id = "gold-frame",
            name = "Gold Frame",
            description = "A shiny gold frame for your profile.",
            image = "https://cdn.example.com/frames/gold.png",
            info = null,
            price = 1000,
            version = "1.0",
            type = StoreItemType.FRAME
        ),
        boughtAt = "2026-07-22T10:00:00Z",
    ),
    OwnedItem(
        id = "inventory-aurora-frame",
        item = StoreItem(
            id = "aurora-frame",
            name = "Aurora Frame",
            description = "A glowing animated frame for your avatar.",
            image = "https://cdn.example.com/frames/aurora.png",
            info = null,
            price = 2000,
            version = "1.0",
            type = StoreItemType.FRAME
        ),
        boughtAt = "2026-07-25T10:00:00Z",
    ),
    OwnedItem(
        id = "inventory-dawn-skin",
        item = StoreItem(
            id = "dawn-skin",
            name = "Dawn Skin",
            description = "A warm Awan skin.",
            image = "https://cdn.example.com/skins/dawn.png",
            info = null,
            price = 500,
            version = "1.0",
            type = StoreItemType.SKIN
        ),
        boughtAt = "2026-07-20T10:00:00Z",
    ),
)

@Preview(name = "Inventory · Mock data", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
fun InventoryScreenPreview() {
    AwanTheme {
        InventoryScreen(
            state = InventoryState(
                items = previewInventory,
                equippedItemIds = setOf("inventory-gold-frame"),
                isLoading = false,
                isOnline = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Inventory · Details edge + bottom blend", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
fun InventoryDetailsSheetPreview() {
    AwanTheme {
        CustomizationDetailsSheet(
            item = previewInventory.first(),
            state = InventoryState(
                items = previewInventory,
                equippedItemIds = setOf("gold-frame"),
                isLoading = false,
                isOnline = true,
            ),
            onEquip = {},
        )
    }
}
