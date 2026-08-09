package com.awan.app.core.data.marketplace

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

fun StoreItemType.asDto(): StoreItemTypeDto = when (this) {
    StoreItemType.FRAME -> StoreItemTypeDto.FRAME
    StoreItemType.SKIN -> StoreItemTypeDto.SKIN
    StoreItemType.THEME -> StoreItemTypeDto.THEME
    StoreItemType.ICON -> StoreItemTypeDto.ICON
}

fun StoreItemDto.asExternalModel(): StoreItem = StoreItem(
    id = id,
    name = name,
    description = description,
    image = image,
    info = info,
    price = price,
    version = version,
    type = type.asExternalModel()
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
