package com.awan.feature.inventory.impl.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.feature.inventory.impl.presentation.InventoryState

private val previewInventory = listOf(
    OwnedCustomization(
        inventoryId = "inventory-gold-frame",
        itemId = "gold-frame",
        name = "Gold Frame",
        description = "A shiny gold frame for your profile.",
        imageUrl = "https://cdn.example.com/frames/gold.png",
        type = CustomizationType.FRAME,
        rarity = CustomizationRarity.RARE,
        acquiredAt = "2026-07-22T10:00:00Z",
        isEquipped = true,
    ),
    OwnedCustomization(
        inventoryId = "inventory-aurora-frame",
        itemId = "aurora-frame",
        name = "Aurora Frame",
        description = "A glowing animated frame for your avatar.",
        imageUrl = "https://cdn.example.com/frames/aurora.png",
        type = CustomizationType.FRAME,
        rarity = CustomizationRarity.EPIC,
        acquiredAt = "2026-07-25T10:00:00Z",
        isEquipped = false,
    ),
    OwnedCustomization(
        inventoryId = "inventory-dawn-skin",
        itemId = "dawn-skin",
        name = "Dawn Skin",
        description = "A warm Awan skin.",
        imageUrl = null,
        type = CustomizationType.SKIN,
        rarity = CustomizationRarity.COMMON,
        acquiredAt = "2026-07-20T10:00:00Z",
        isEquipped = false,
    ),
)

@Preview(name = "Inventory · Mock data", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
fun InventoryScreenPreview() {
    AwanTheme {
        InventoryScreen(
            state = InventoryState(
                customizations = previewInventory,
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
            customization = previewInventory.first(),
            state = InventoryState(
                customizations = previewInventory,
                isLoading = false,
                isOnline = true,
            ),
            onEquip = {},
        )
    }
}
