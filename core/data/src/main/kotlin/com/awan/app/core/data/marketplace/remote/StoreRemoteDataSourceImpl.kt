package com.awan.app.core.data.marketplace.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.StoreApiService
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class StoreRemoteDataSourceImpl @Inject constructor(
    private val storeApiService: StoreApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : StoreRemoteDataSource {

    override suspend fun getStoreItems(type: StoreItemTypeDto?): Result<List<StoreItemDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.getStoreItems(type?.name)
        }

    override suspend fun getInventory(): Result<List<OwnedItemDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.getInventory()
        }

    override suspend fun buyItem(itemId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.buyItem(itemId)
        }

    override suspend fun getEquippedItems(): Result<List<EquippedItemDto>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.getEquippedItems()
        }

    override suspend fun equipItem(itemId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.equipItem(itemId)
        }

    override suspend fun unequipItem(itemType: StoreItemTypeDto): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            storeApiService.unequipItem(itemType.name)
        }
}
