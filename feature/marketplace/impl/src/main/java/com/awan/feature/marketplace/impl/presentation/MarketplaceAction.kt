package com.awan.feature.marketplace.impl.presentation

import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType

sealed interface MarketplaceAction {
    data class SelectCategory(val category: StoreItemType?) : MarketplaceAction
    data class UpdateSearchQuery(val query: String) : MarketplaceAction
    data class SelectItem(val item: StoreItem?) : MarketplaceAction
    data class BuyItem(val item: StoreItem) : MarketplaceAction
    data class EquipItem(val item: StoreItem) : MarketplaceAction
    data class UnequipItem(val type: StoreItemType) : MarketplaceAction
    data object Refresh : MarketplaceAction
    data object DismissError : MarketplaceAction
}
