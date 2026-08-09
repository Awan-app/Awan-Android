package com.awan.app.core.domain.inventory.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeInventory(): Flow<List<OwnedCustomization>>

    fun observeEquippedFrame(): Flow<String?>

    suspend fun refresh(): Result<Unit>

    suspend fun equip(itemId: String): Result<Unit>
}
