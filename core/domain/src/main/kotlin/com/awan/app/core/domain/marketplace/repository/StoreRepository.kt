package com.awan.app.core.domain.marketplace.repository

import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.common.result.Result
import kotlinx.coroutines.flow.Flow

interface StoreRepository {
    fun getStoreItems(type: StoreItemType? = null): Flow<List<StoreItem>>
    fun getInventory(): Flow<List<OwnedItem>>
    fun getEquippedItems(): Flow<List<EquippedItem>>
    suspend fun buyItem(itemId: String): Result<Unit>
    suspend fun equipItem(itemId: String): Result<Unit>
    suspend fun unequipItem(itemType: StoreItemType): Result<Unit>
    suspend fun refreshStoreItems(type: StoreItemType? = null)
    suspend fun refreshInventory(): Result<Unit>
    suspend fun refreshEquippedItems(): Result<Unit>
}
