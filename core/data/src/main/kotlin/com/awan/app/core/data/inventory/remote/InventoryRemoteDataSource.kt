package com.awan.app.core.data.inventory.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.inventory.EquippedItemResponse
import com.awan.app.core.network.dto.inventory.InventoryItemResponse

interface InventoryRemoteDataSource {
    suspend fun getInventory(): Result<List<InventoryItemResponse>>

    suspend fun getEquipped(): Result<List<EquippedItemResponse>>

    suspend fun equip(itemId: String): Result<EquippedItemResponse>
}
