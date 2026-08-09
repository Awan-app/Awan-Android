package com.awan.app.core.data.marketplace.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto

interface StoreRemoteDataSource {
    suspend fun getStoreItems(type: StoreItemTypeDto? = null): Result<List<StoreItemDto>>
    suspend fun getInventory(): Result<List<OwnedItemDto>>
    suspend fun buyItem(itemId: String): Result<Unit>
    suspend fun getEquippedItems(): Result<List<EquippedItemDto>>
    suspend fun equipItem(itemId: String): Result<Unit>
    suspend fun unequipItem(itemType: StoreItemTypeDto): Result<Unit>
}
