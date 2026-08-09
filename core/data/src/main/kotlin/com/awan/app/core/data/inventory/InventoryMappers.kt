package com.awan.app.core.data.inventory

import com.awan.app.core.database.model.OwnedCustomizationEntity
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.app.core.network.dto.inventory.InventoryItemResponse

internal fun InventoryItemResponse.toEntity(
    userId: String,
    equippedItemIds: Set<String>,
): OwnedCustomizationEntity = OwnedCustomizationEntity(
    userId = userId,
    inventoryId = id,
    itemId = item.id,
    name = item.name,
    description = item.description,
    imageUrl = item.image,
    type = item.type.trim().uppercase(),
    rarity = CustomizationRarity.fromInfo(item.info).name,
    acquiredAt = boughtAt,
    isEquipped = item.id in equippedItemIds,
)

internal fun OwnedCustomizationEntity.asExternalModel(): OwnedCustomization = OwnedCustomization(
    inventoryId = inventoryId,
    itemId = itemId,
    name = name,
    description = description,
    imageUrl = imageUrl,
    type = CustomizationType.fromWireValue(type),
    rarity = CustomizationRarity.entries.firstOrNull { it.name == rarity } ?: CustomizationRarity.UNKNOWN,
    acquiredAt = acquiredAt,
    isEquipped = isEquipped,
)
