package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization

enum class InventorySort {
    RARITY,
    NEWEST,
    NAME,
}

data class InventorySection(
    val type: CustomizationType,
    val items: List<OwnedCustomization>,
)

data class InventoryState(
    val customizations: List<OwnedCustomization> = emptyList(),
    val selectedType: CustomizationType? = null,
    val selectedRarities: Set<CustomizationRarity> = emptySet(),
    val sort: InventorySort = InventorySort.RARITY,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isOnline: Boolean = true,
    val equippingItemId: String? = null,
    val error: UiText? = null,
) {
    val sections: List<InventorySection>
        get() = inventorySections(customizations, selectedType, selectedRarities, sort)
}

internal fun inventorySections(
    customizations: List<OwnedCustomization>,
    selectedType: CustomizationType?,
    selectedRarities: Set<CustomizationRarity>,
    sort: InventorySort,
): List<InventorySection> = customizations
    .asSequence()
    .filter { selectedType == null || it.type == selectedType }
    .filter { selectedRarities.isEmpty() || it.rarity in selectedRarities }
    .groupBy { it.type }
    .toSortedMap(compareBy { it.ordinal })
    .map { (type, items) -> InventorySection(type, items.sortedFor(sort)) }

private fun List<OwnedCustomization>.sortedFor(sort: InventorySort): List<OwnedCustomization> = when (sort) {
    InventorySort.RARITY -> sortedWith(
        knownRaritiesFirst
            .thenByDescending { it.rarity.rank }
            .thenByDescending { it.acquiredAt }
            .thenBy { it.name },
    )
    InventorySort.NEWEST -> sortedWith(knownRaritiesFirst.thenByDescending { it.acquiredAt }.thenBy { it.name })
    InventorySort.NAME -> sortedWith(knownRaritiesFirst.thenBy { it.name.lowercase() })
}

private val knownRaritiesFirst = compareBy<OwnedCustomization> { it.rarity == CustomizationRarity.UNKNOWN }
