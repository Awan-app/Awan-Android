package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItemType

enum class InventorySort {
    RARITY,
    NEWEST,
    NAME,
}

data class InventoryItem(
    val itemId: String,
    val name: String,
    val description: String,
    val imageUrl: String?,
    val type: StoreItemType,
    val rarity: CustomizationRarity,
    val price: Int,
    val isEquipped: Boolean,
    val isSeen: Boolean,
    val acquiredAt: String,
)

data class InventorySection(
    val type: StoreItemType,
    val items: List<InventoryItem>,
)

data class InventoryState(
    val items: List<OwnedItem> = emptyList(),
    val equippedItemIds: Set<String> = emptySet(),
    val selectedType: StoreItemType? = null,
    val selectedRarities: Set<CustomizationRarity> = emptySet(),
    val sort: InventorySort = InventorySort.NEWEST,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val equippingItemId: String? = null,
    val unequippingType: StoreItemType? = null,
    val detailsItemId: String? = null,
    val unseenItemIds: Set<String> = emptySet(),
    val error: UiText? = null,
) {
    val sections: List<InventorySection>
        get() = inventorySections(items, equippedItemIds, selectedType, selectedRarities, sort)

    val detailsItem: OwnedItem?
        get() = items.firstOrNull { it.item.id == detailsItemId }

    fun isTypeEquipped(type: StoreItemType): Boolean =
        items.any { it.item.type == type && it.item.id in equippedItemIds }
}

internal fun inventorySections(
    items: List<OwnedItem>,
    equippedItemIds: Set<String>,
    selectedType: StoreItemType?,
    selectedRarities: Set<CustomizationRarity> = emptySet(),
    sort: InventorySort = InventorySort.NEWEST,
): List<InventorySection> = items
    .asSequence()
    .filter { selectedType == null || it.item.type == selectedType }
    .filter {
        selectedRarities.isEmpty() ||
            CustomizationRarity.fromInfo(it.item.info) in selectedRarities
    }
    .map { ownedItem ->
        InventoryItem(
            itemId = ownedItem.item.id,
            name = ownedItem.item.name,
            description = ownedItem.item.description,
            imageUrl = ownedItem.item.image,
            type = ownedItem.item.type,
            rarity = CustomizationRarity.fromInfo(ownedItem.item.info),
            price = ownedItem.item.price,
            isEquipped = ownedItem.item.id in equippedItemIds,
            isSeen = ownedItem.isSeen,
            acquiredAt = ownedItem.boughtAt,
        )
    }
    .groupBy { it.type }
    .toSortedMap(compareBy { it.ordinal })
    .map { (type, items) -> InventorySection(type, items.sortedFor(sort)) }

private fun List<InventoryItem>.sortedFor(sort: InventorySort): List<InventoryItem> = when (sort) {
    InventorySort.RARITY -> sortedWith(
        compareByDescending<InventoryItem> { rarityRank(it.rarity) }
            .thenByDescending { it.acquiredAt }
            .thenBy { it.name.lowercase() }
    )
    InventorySort.NEWEST -> sortedWith(compareByDescending<InventoryItem> { it.acquiredAt }.thenBy { it.name })
    InventorySort.NAME -> sortedBy { it.name.lowercase() }
}

private fun rarityRank(rarity: CustomizationRarity): Int = when (rarity) {
    CustomizationRarity.LEGENDARY -> 5
    CustomizationRarity.EPIC -> 4
    CustomizationRarity.RARE -> 3
    CustomizationRarity.UNCOMMON -> 2
    CustomizationRarity.COMMON -> 1
    CustomizationRarity.UNKNOWN -> 0
}

