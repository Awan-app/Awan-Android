package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType

sealed interface InventoryAction {
    data object Refresh : InventoryAction
    data class SelectType(val type: CustomizationType?) : InventoryAction
    data class ToggleRarity(val rarity: CustomizationRarity) : InventoryAction
    data class SetSort(val sort: InventorySort) : InventoryAction
    data class Equip(val itemId: String) : InventoryAction
}
