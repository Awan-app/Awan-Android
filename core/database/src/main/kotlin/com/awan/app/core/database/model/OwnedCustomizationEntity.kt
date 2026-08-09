package com.awan.app.core.database.model

import androidx.room.Entity

@Entity(
    tableName = "owned_customizations",
    primaryKeys = ["userId", "itemId"],
)
data class OwnedCustomizationEntity(
    val userId: String,
    val inventoryId: String,
    val itemId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val type: String,
    val rarity: String,
    val acquiredAt: String,
    val isEquipped: Boolean,
)
