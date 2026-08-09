package com.awan.app.core.domain.inventory.model

data class OwnedCustomization(
    val inventoryId: String,
    val itemId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val type: CustomizationType,
    val rarity: CustomizationRarity,
    val acquiredAt: String,
    val isEquipped: Boolean,
)
