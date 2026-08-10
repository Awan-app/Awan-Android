package com.awan.app.core.model

enum class StoreItemType {
    FRAME,
    SKIN,
    THEME,
    ICON
}

data class StoreItem(
    val id: String,
    val name: String,
    val description: String,
    val image: String,
    val info: String?,
    val price: Int,
    val version: String,
    val type: StoreItemType
)

data class OwnedItem(
    val id: String,
    val item: StoreItem,
    val boughtAt: String
)

data class EquippedItem(
    val type: StoreItemType,
    val item: StoreItem,
    val equippedAt: String
)
