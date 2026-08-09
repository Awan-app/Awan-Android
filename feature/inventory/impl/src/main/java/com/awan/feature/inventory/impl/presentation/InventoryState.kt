package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItemType

enum class InventorySort {
    NEWEST,
    NAME,
}

data class InventoryItem(
    val itemId: String,
    val name: String,
    val imageUrl: String?,
    val type: StoreItemType,
    val isEquipped: Boolean,
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
    val sort: InventorySort = InventorySort.NEWEST,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val equippingItemId: String? = null,
    val error: UiText? = null,
) {
    val sections: List<InventorySection>
        get() = inventorySections(items, equippedItemIds, selectedType, sort)
}

internal fun inventorySections(
    items: List<OwnedItem>,
    equippedItemIds: Set<String>,
    selectedType: StoreItemType?,
    sort: InventorySort,
): List<InventorySection> = items
    .asSequence()
    .filter { selectedType == null || it.item.type == selectedType }
    .map { ownedItem ->
        InventoryItem(
            itemId = ownedItem.item.id,
            name = ownedItem.item.name,
            imageUrl = ownedItem.item.image,
            type = ownedItem.item.type,
            isEquipped = ownedItem.item.id in equippedItemIds,
            acquiredAt = ownedItem.boughtAt,
        )
    }
    .groupBy { it.type }
    .toSortedMap(compareBy { it.ordinal })
    .map { (type, items) -> InventorySection(type, items.sortedFor(sort)) }

private fun List<InventoryItem>.sortedFor(sort: InventorySort): List<InventoryItem> = when (sort) {
    InventorySort.NEWEST -> sortedWith(compareByDescending<InventoryItem> { it.acquiredAt }.thenBy { it.name })
    InventorySort.NAME -> sortedBy { it.name.lowercase() }
}
