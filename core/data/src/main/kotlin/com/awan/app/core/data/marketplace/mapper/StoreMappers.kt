package com.awan.app.core.data.marketplace

import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto

fun StoreItemTypeDto.asExternalModel(): StoreItemType = when (this) {
    StoreItemTypeDto.FRAME -> StoreItemType.FRAME
    StoreItemTypeDto.SKIN -> StoreItemType.SKIN
    StoreItemTypeDto.THEME -> StoreItemType.THEME
    StoreItemTypeDto.ICON -> StoreItemType.ICON
}

fun String?.asStoreItemType(): StoreItemType = when (this) {
    "FRAME" -> StoreItemType.FRAME
    "SKIN" -> StoreItemType.SKIN
    "THEME" -> StoreItemType.THEME
    "ICON" -> StoreItemType.ICON
    else -> StoreItemType.FRAME // Default
}

fun StoreItemType.asDto(): StoreItemTypeDto = when (this) {
    StoreItemType.FRAME -> StoreItemTypeDto.FRAME
    StoreItemType.SKIN -> StoreItemTypeDto.SKIN
    StoreItemType.THEME -> StoreItemTypeDto.THEME
    StoreItemType.ICON -> StoreItemTypeDto.ICON
}

fun StoreItemDto.asExternalModel(): StoreItem = StoreItem(
    id = id,
    name = name ?: "",
    description = description ?: "",
    image = image ?: "",
    info = info,
    price = price,
    version = version ?: "",
    type = type.asStoreItemType()
)

fun OwnedItemDto.asExternalModel(): OwnedItem = OwnedItem(
    id = id,
    item = item.asExternalModel(),
    boughtAt = boughtAt
)

fun EquippedItemDto.asExternalModel(): EquippedItem = EquippedItem(
    type = type.asExternalModel(),
    item = item.asExternalModel(),
    equippedAt = equippedAt
)

// Entity mappings
fun StoreItemEntity.asExternalModel(): StoreItem = StoreItem(
    id = id,
    name = name,
    description = description,
    image = image,
    info = info,
    price = price,
    version = version,
    type = type.asStoreItemType()
)

fun StoreItem.asEntity(expiryTime: Long = 0L): StoreItemEntity = StoreItemEntity(
    id = id,
    name = name,
    description = description,
    image = image,
    info = info,
    price = price,
    version = version,
    type = type.name,
    expiryTime = expiryTime
)

fun OwnedItemEntity.asExternalModel(items: List<StoreItem>): OwnedItem? {
    val storeItem = items.find { it.id == itemId } ?: return null
    return OwnedItem(
        id = id,
        item = storeItem,
        boughtAt = boughtAt
    )
}

fun OwnedItem.asEntity(expiryTime: Long = 0L): OwnedItemEntity = OwnedItemEntity(
    id = id,
    itemId = item.id,
    boughtAt = boughtAt,
    expiryTime = expiryTime
)

fun EquippedItemEntity.asExternalModel(items: List<StoreItem>): EquippedItem? {
    val storeItem = items.find { it.id == itemId } ?: return null
    return EquippedItem(
        type = type.asStoreItemType(),
        item = storeItem,
        equippedAt = equippedAt
    )
}

fun EquippedItem.asEntity(expiryTime: Long = 0L): EquippedItemEntity = EquippedItemEntity(
    type = type.name,
    itemId = item.id,
    equippedAt = equippedAt,
    expiryTime = expiryTime
)
