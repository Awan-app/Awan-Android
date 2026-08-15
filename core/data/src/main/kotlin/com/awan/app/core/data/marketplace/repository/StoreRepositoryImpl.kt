package com.awan.app.core.data.marketplace.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.marketplace.asDto
import com.awan.app.core.data.marketplace.asEntity
import com.awan.app.core.data.marketplace.asExternalModel
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.data.sync.SyncTtl
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

import com.awan.app.core.data.gamification.GamificationEventBus

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val remoteDataSource: StoreRemoteDataSource,
    private val storeDao: StoreDao,
    private val profileRepository: ProfileRepository,
    private val gamificationEventBus: GamificationEventBus,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {

    override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> =
        if (type == null) {
            storeDao.observeStoreItems().map { entities -> entities.mapNotNull { it.asExternalModel() } }
        } else {
            storeDao.observeStoreItemsByType(type.name).map { entities -> entities.mapNotNull { it.asExternalModel() } }
        }

    override fun getInventory(): Flow<List<OwnedItem>> =
        combine(
            storeDao.observeOwnedItems(),
            storeDao.observeStoreItems()
        ) { ownedEntities, itemEntities ->
            val items = itemEntities.mapNotNull { it.asExternalModel() }
            ownedEntities.mapNotNull { it.asExternalModel(items) }
        }

    override fun getEquippedItems(): Flow<List<EquippedItem>> =
        combine(
            storeDao.observeEquippedItems(),
            storeDao.observeStoreItems()
        ) { equippedEntities, itemEntities ->
            val items = itemEntities.mapNotNull { it.asExternalModel() }
            equippedEntities.mapNotNull { it.asExternalModel(items) }
        }

    override suspend fun buyItem(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.buyItem(itemId)
        if (result is Result.Success) {
            // Refresh inventory and profile
            val invResult = refreshInventory()
            if (invResult is Result.Error) return@withContext invResult

            val profileResult = profileRepository.getProfile()
            when (profileResult) {
                is Result.Success -> {
                    val points = profileResult.data.points ?: 0
                    gamificationEventBus.updatePoints(points)
                }
                is Result.Error -> return@withContext Result.Error(profileResult.error)
                Result.Loading -> return@withContext Result.Error(AppError.Unknown(IllegalStateException("Unexpected loading state during profile sync")))
            }
        }
        result
    }

    override suspend fun equipItem(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.equipItem(itemId)
        if (result is Result.Success) {
            refreshEquippedItems()
        }
        result
    }

    override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> =
        withContext(ioDispatcher) {
            if (!connectivityMonitor.isCurrentlyOnline()) {
                return@withContext Result.Error(AppError.Network)
            }

            // Find current equipped item ID for this type
            val equipped = storeDao.observeEquippedItems().first()
            val item = equipped.find { it.type == itemType.name }

            if (item != null) {
                val result = remoteDataSource.unequipItem(item.itemId)
                if (result is Result.Success) {
                    storeDao.deleteEquippedItemByType(itemType.name)
                }
                result
            } else {
                Result.Success(Unit)
            }
        }

    override suspend fun refreshStoreItems(type: StoreItemType?) = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext

        val result = remoteDataSource.getStoreItems(type?.asDto())
        if (result is Result.Success) {
            val expiry = SyncTtl.computeExpiry(SyncTtl.STORE_TTL_MS)
            val entities = result.data.mapNotNull { it.asExternalModel()?.asEntity(expiry) }
            if (type == null) {
                storeDao.replaceStoreItems(entities)
            } else {
                storeDao.upsertStoreItems(entities)
            }
        }
    }

    override suspend fun refreshInventory(): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext Result.Error(AppError.Network)

        val result = remoteDataSource.getInventory()
        if (result is Result.Success) {
            val expiry = SyncTtl.computeExpiry(SyncTtl.STORE_TTL_MS)
            val entities = result.data.map { dto ->
                OwnedItemEntity(
                    id = dto.id,
                    itemId = dto.item.id,
                    boughtAt = dto.boughtAt,
                    expiryTime = expiry
                )
            }
            storeDao.replaceOwnedItemsPreservingSeen(entities)

            // Also ensure store items from inventory are in store_items table
            val storeItems = result.data.mapNotNull { it.item.asExternalModel()?.asEntity(expiry) }
            storeDao.upsertStoreItems(storeItems)
            Result.Success(Unit)
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun refreshEquippedItems(): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) return@withContext Result.Error(AppError.Network)

        val result = remoteDataSource.getEquippedItems()
        if (result is Result.Success) {
            val expiry = SyncTtl.computeExpiry(SyncTtl.STORE_TTL_MS)
            val entities = result.data.map { dto ->
                EquippedItemEntity(
                    type = dto.type.name,
                    itemId = dto.item.id,
                    equippedAt = dto.equippedAt,
                    expiryTime = expiry
                )
            }
            storeDao.replaceEquippedItems(entities)

            // Also ensure store items from equipped are in store_items table
            val storeItems = result.data.mapNotNull { it.item.asExternalModel()?.asEntity(expiry) }
            storeDao.upsertStoreItems(storeItems)
            Result.Success(Unit)
        } else {
            Result.Error((result as Result.Error).error)
        }
    }

    override suspend fun markInventorySeen() = withContext(ioDispatcher) {
        storeDao.markAllOwnedItemsSeen()
    }
}