package com.awan.app.core.data.marketplace

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreRepositoryImpl @Inject constructor(
    private val remoteDataSource: StoreRemoteDataSource,
    private val profileRepository: ProfileRepository,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : StoreRepository {

    override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = flow {
        if (connectivityMonitor.isCurrentlyOnline()) {
            val result = remoteDataSource.getStoreItems(type?.asDto())
            if (result is Result.Success) {
                emit(result.data.map { it.asExternalModel() })
            } else {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    override fun getInventory(): Flow<List<OwnedItem>> = flow {
        if (connectivityMonitor.isCurrentlyOnline()) {
            val result = remoteDataSource.getInventory()
            if (result is Result.Success) {
                emit(result.data.map { it.asExternalModel() })
            } else {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    override fun getEquippedItems(): Flow<List<EquippedItem>> = flow {
        if (connectivityMonitor.isCurrentlyOnline()) {
            val result = remoteDataSource.getEquippedItems()
            if (result is Result.Success) {
                emit(result.data.map { it.asExternalModel() })
            } else {
                emit(emptyList())
            }
        } else {
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)

    override suspend fun buyItem(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        val result = remoteDataSource.buyItem(itemId)
        if (result is Result.Success) {
            // Refresh profile to update points
            profileRepository.getProfile()
        }
        result
    }

    override suspend fun equipItem(itemId: String): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.equipItem(itemId)
    }

    override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> = withContext(ioDispatcher) {
        if (!connectivityMonitor.isCurrentlyOnline()) {
            return@withContext Result.Error(AppError.Network)
        }
        remoteDataSource.unequipItem(itemType.asDto())
    }
}
