package com.awan.app.core.data.inventory.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.StoreApiService
import com.awan.app.core.network.dto.inventory.EquippedItemResponse
import com.awan.app.core.network.dto.inventory.InventoryItemResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class InventoryRemoteDataSourceImpl @Inject constructor(
    private val storeApiService: StoreApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : InventoryRemoteDataSource {
    override suspend fun getInventory(): Result<List<InventoryItemResponse>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) { storeApiService.getInventory() }

    override suspend fun getEquipped(): Result<List<EquippedItemResponse>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) { storeApiService.getEquipped() }

    override suspend fun equip(itemId: String): Result<EquippedItemResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) { storeApiService.equip(itemId) }
}
