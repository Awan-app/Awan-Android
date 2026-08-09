package com.awan.feature.marketplace.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeStoreRepository : StoreRepository {
    val storeItemsFlow = MutableStateFlow<List<StoreItem>>(emptyList())
    val inventoryFlow = MutableStateFlow<List<OwnedItem>>(emptyList())
    val equippedItemsFlow = MutableStateFlow<List<EquippedItem>>(emptyList())
    
    var failBuyWith: Result<Unit>? = null
    var failEquipWith: Result<Unit>? = null
    var failUnequipWith: Result<Unit>? = null

    override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = storeItemsFlow

    override fun getInventory(): Flow<List<OwnedItem>> = inventoryFlow

    override fun getEquippedItems(): Flow<List<EquippedItem>> = equippedItemsFlow

    override suspend fun buyItem(itemId: String): Result<Unit> {
        return failBuyWith ?: Result.Success(Unit)
    }

    override suspend fun equipItem(itemId: String): Result<Unit> {
        return failEquipWith ?: Result.Success(Unit)
    }

    override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> {
        return failUnequipWith ?: Result.Success(Unit)
    }
}
