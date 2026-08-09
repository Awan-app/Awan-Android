package com.awan.app.core.data.inventory

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.data.inventory.remote.InventoryRemoteDataSource
import com.awan.app.core.database.dao.OwnedCustomizationDao
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.app.core.domain.inventory.repository.InventoryRepository
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InventoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: InventoryRemoteDataSource,
    private val ownedCustomizationDao: OwnedCustomizationDao,
    private val authTokenProvider: AuthTokenProvider,
    private val connectivityMonitor: NetworkConnectivityMonitor,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : InventoryRepository {
    private val writeMutex = Mutex()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeInventory(): Flow<List<OwnedCustomization>> =
        authTokenProvider.observeIsLoggedIn().flatMapLatest { isLoggedIn ->
            if (!isLoggedIn) {
                flowOf(emptyList())
            } else {
                flow {
                    val userId = authTokenProvider.getUserId()
                    if (userId == null) emit(emptyList()) else emitAll(
                        ownedCustomizationDao.observeForUser(userId).map { entities -> entities.map { it.asExternalModel() } },
                    )
                }
            }
        }.flowOn(ioDispatcher)

    override fun observeEquippedFrame(): Flow<String?> =
        observeInventory().map { customizations ->
            customizations.firstOrNull { it.type == CustomizationType.FRAME && it.isEquipped }?.imageUrl
        }

    override suspend fun refresh(): Result<Unit> = writeMutex.withLock { refreshLocked() }

    private suspend fun refreshLocked(): Result<Unit> {
        if (!connectivityMonitor.isCurrentlyOnline()) return Result.Error(AppError.Network)
        val userId = authTokenProvider.getUserId() ?: return Result.Error(AppError.Unauthorized)
        val inventory = remoteDataSource.getInventory()
        if (inventory !is Result.Success) return inventory.asUnit()
        val equipped = remoteDataSource.getEquipped()
        if (equipped !is Result.Success) return equipped.asUnit()
        if (authTokenProvider.getUserId() != userId) return Result.Error(AppError.Unauthorized)

        val equippedItemIds = equipped.data.mapTo(mutableSetOf()) { it.item.id }
        ownedCustomizationDao.replaceForUser(
            userId = userId,
            customizations = inventory.data.map { it.toEntity(userId, equippedItemIds) },
        )
        return Result.Success(Unit)
    }

    override suspend fun equip(itemId: String): Result<Unit> = writeMutex.withLock {
        if (!connectivityMonitor.isCurrentlyOnline()) return Result.Error(AppError.Network)
        val userId = authTokenProvider.getUserId() ?: return Result.Error(AppError.Unauthorized)
        val result = remoteDataSource.equip(itemId)
        if (result !is Result.Success) {
            val error = (result as? Result.Error)?.error
            if (error is AppError.Api && error.errorCode == "ITEM_NOT_OWNED") {
                refreshLocked()
            }
            return result.asUnit()
        }

        if (authTokenProvider.getUserId() != userId) return Result.Error(AppError.Unauthorized)
        ownedCustomizationDao.setEquipped(
            userId = userId,
            type = result.data.type.trim().uppercase(),
            itemId = result.data.item.id,
        )
        return Result.Success(Unit)
    }

    private fun Result<*>.asUnit(): Result<Unit> = when (this) {
        is Result.Error -> this
        Result.Loading -> Result.Loading
        is Result.Success -> Result.Success(Unit)
    }
}
