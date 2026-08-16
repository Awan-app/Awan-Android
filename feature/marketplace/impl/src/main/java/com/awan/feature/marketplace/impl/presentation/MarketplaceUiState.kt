package com.awan.feature.marketplace.impl.presentation

import androidx.annotation.StringRes
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType

data class MarketplaceUiState(
    val isLoading: Boolean = false,
    val items: List<StoreItem> = emptyList(),
    val inventory: List<OwnedItem> = emptyList(),
    val equipped: List<EquippedItem> = emptyList(),
    val points: Int = 0,
    val selectedCategory: StoreItemType? = null,
    val searchQuery: String = "",
    @StringRes val error: Int? = null,
    val isBuying: Boolean = false,
    val isEquipping: Boolean = false,
    val selectedItem: StoreItem? = null,
    val accessToken: String? = null
) {
    val filteredItems: List<StoreItem> = items.filter { item ->
        (selectedCategory == null || item.type == selectedCategory) &&
        (searchQuery.isEmpty() || item.name.contains(searchQuery, ignoreCase = true))
    }

    val ownedItemIds: Set<String> = inventory.map { it.item.id }.toSet()
    val equippedItemIds: Set<String> = equipped.map { it.item.id }.toSet()
    
    val ownedItems: List<StoreItem> = items.filter { it.id in ownedItemIds }
    val lockedItems: List<StoreItem> = items.filter { it.id !in ownedItemIds }
}
