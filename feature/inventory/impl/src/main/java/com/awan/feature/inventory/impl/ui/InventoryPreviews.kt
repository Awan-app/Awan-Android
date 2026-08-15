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
        id = "inventory-solar-crown",
        item = StoreItem(
            id = "solar-crown",
            name = "Solar Crown",
            description = "A radiant golden crown emitting celestial sunlight.",
            image = "https://cdn.example.com/frames/solar_crown.png",
            info = "rarity: legendary",
            price = 3500,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-08-14T12:00:00Z",
        isSeen = false,
    ),
    OwnedItem(
        id = "inventory-aurora-frame",
        item = StoreItem(
            id = "aurora-frame",
            name = "Aurora Frame",
            description = "A glowing animated frame with dancing polar lights.",
            image = "https://cdn.example.com/frames/aurora.png",
            info = "rarity: epic",
            price = 2000,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-08-10T10:00:00Z",
        isSeen = true,
    ),
    OwnedItem(
        id = "inventory-gold-frame",
        item = StoreItem(
            id = "gold-frame",
            name = "Gold Frame",
            description = "A shiny polished gold frame for your profile.",
            image = "https://cdn.example.com/frames/gold.png",
            info = "rarity: rare",
            price = 1000,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-07-22T10:00:00Z",
        isSeen = true,
    ),
    OwnedItem(
        id = "inventory-forest-frame",
        item = StoreItem(
            id = "forest-frame",
            name = "Forest Frame",
            description = "Woven vines and emerald foliage from deep woods.",
            image = "https://cdn.example.com/frames/forest.png",
            info = "rarity: uncommon",
            price = 600,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-07-15T08:00:00Z",
        isSeen = true,
    ),
    OwnedItem(
        id = "inventory-wooden-frame",
        item = StoreItem(
            id = "wooden-frame",
            name = "Wooden Frame",
            description = "A simple, clean handcrafted wooden border.",
            image = "https://cdn.example.com/frames/wood.png",
            info = "rarity: common",
            price = 200,
            version = "1.0",
            type = StoreItemType.FRAME,
        ),
        boughtAt = "2026-07-01T08:00:00Z",
        isSeen = true,
    ),
    OwnedItem(
        id = "inventory-dawn-skin",
        item = StoreItem(
            id = "dawn-skin",
            name = "Dawn Skin",
            description = "A warm Awan cloud skin glowing with early morning hues.",
            image = "https://cdn.example.com/skins/dawn.png",
            info = "rarity: rare",
            price = 800,
            version = "1.0",
            type = StoreItemType.SKIN,
        ),
        boughtAt = "2026-07-20T10:00:00Z",
        isSeen = true,
    ),
    OwnedItem(
        id = "inventory-midnight-theme",
        item = StoreItem(
            id = "midnight-theme",
            name = "Midnight Nebula",
            description = "Deep violet and starry space theme for the entire app.",
            image = "https://cdn.example.com/themes/midnight.png",
            info = "rarity: epic",
            price = 1500,
            version = "1.0",
            type = StoreItemType.THEME,
        ),
        boughtAt = "2026-08-12T16:00:00Z",
        isSeen = false,
    ),
)

@Preview(name = "Inventory · Mixed Rarities, Equipped & Unseen Items", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
fun InventoryScreenMixedRaritiesPreview() {
    AwanTheme {
        InventoryScreen(
            state = InventoryState(
                items = previewInventory,
                equippedItemIds = setOf("gold-frame"),
                unseenItemIds = setOf("solar-crown", "midnight-theme"),
                isLoading = false,
                isOnline = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Inventory · Details Sheet (Unequipped / Equip)", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
fun InventoryDetailsSheetUnequippedPreview() {
    AwanTheme {
        CustomizationDetailsSheet(
            item = previewInventory.first { it.item.id == "solar-crown" },
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

@Preview(name = "Inventory · Details Sheet (Equipped / Already Equipped)", showBackground = true, widthDp = 420, heightDp = 720)
@Composable
fun InventoryDetailsSheetEquippedPreview() {
    AwanTheme {
        CustomizationDetailsSheet(
            item = previewInventory.first { it.item.id == "gold-frame" },
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

@Preview(name = "Inventory · Empty State", showBackground = true, widthDp = 420, heightDp = 600)
@Composable
fun InventoryScreenEmptyPreview() {
    AwanTheme {
        InventoryScreen(
            state = InventoryState(
                items = emptyList(),
                isLoading = false,
                isOnline = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Inventory · Offline State", showBackground = true, widthDp = 420, heightDp = 600)
@Composable
fun InventoryScreenOfflinePreview() {
    AwanTheme {
        InventoryScreen(
            state = InventoryState(
                items = emptyList(),
                isLoading = false,
                isOnline = false,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
