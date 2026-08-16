package com.awan.app.core.data.inventory

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.error.AppError
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.data.marketplace.repository.StoreRepositoryImpl
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto
import com.awan.app.core.data.gamification.GamificationEventBus
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.model.UserEntity
import com.awan.app.core.database.model.UserPreferencesEntity
import com.awan.app.core.database.model.UserWithPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val frame = StoreItemDto(
        id = "frame-1",
        name = "Aurora Frame",
        description = "A glowing frame",
        image = "https://example.com/aurora.png",
        info = "Rarity: epic",
        price = 200,
        version = "1.0",
        type = "FRAME",
    )
    
    private lateinit var remote: FakeStoreRemoteDataSource
    private lateinit var dao: FakeStoreDao
    private lateinit var repository: StoreRepositoryImpl

    @Before
    fun setup() {
        remote = FakeStoreRemoteDataSource(
            inventory = listOf(OwnedItemDto(id = "owned-1", item = frame, boughtAt = "2026-08-01T10:00:00Z")),
            equipped = listOf(EquippedItemDto(type = StoreItemTypeDto.FRAME, item = frame, equippedAt = "2026-08-02T10:00:00Z")),
        )
        dao = FakeStoreDao()
        repository = StoreRepositoryImpl(
            remoteDataSource = remote,
            storeDao = dao,
            profileRepository = FakeProfileRepository(),
            gamificationEventBus = GamificationEventBus(FakeUserDao()),
            connectivityMonitor = FakeNetworkConnectivityMonitor(),
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `refreshInventory stores the items in DAO`() = runTest {
        repository.refreshInventory()

        assertEquals(1, dao.ownedItems.size)
        assertEquals("owned-1", dao.ownedItems[0].id)
        assertEquals("frame-1", dao.ownedItems[0].itemId)
    }

    @Test
    fun `equipItem calls remote`() = runTest {
        val result = repository.equipItem("frame-1")

        assertTrue(result is Result.Success<*>)
        assertTrue(remote.equipCalled)
    }

    private class FakeStoreRemoteDataSource(
        private val inventory: List<OwnedItemDto>,
        private val equipped: List<EquippedItemDto>,
    ) : StoreRemoteDataSource {
        var equipCalled = false
        var equipResult: Result<Unit> = Result.Success(Unit)

        override suspend fun getStoreItems(type: StoreItemTypeDto?): Result<List<StoreItemDto>> = Result.Success(emptyList())
        override suspend fun getInventory(): Result<List<OwnedItemDto>> = Result.Success(inventory)
        override suspend fun getEquippedItems(): Result<List<EquippedItemDto>> = Result.Success(equipped)
        override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun equipItem(itemId: String): Result<Unit> {
            equipCalled = true
            return equipResult
        }
        override suspend fun unequipItem(type: String): Result<Unit> = Result.Success(Unit)
    }

    private class FakeStoreDao : StoreDao {
        var storeItems = listOf<StoreItemEntity>()
        var ownedItems = listOf<OwnedItemEntity>()
        var equippedItems = listOf<EquippedItemEntity>()

        override suspend fun upsertStoreItems(items: List<StoreItemEntity>) { storeItems = items }
        override fun observeStoreItems(): Flow<List<StoreItemEntity>> = flowOf(storeItems)
        override fun observeStoreItemsByType(type: String): Flow<List<StoreItemEntity>> = flowOf(storeItems.filter { it.type == type })
        override suspend fun deleteAllStoreItems() { storeItems = emptyList() }
        override suspend fun upsertOwnedItems(items: List<OwnedItemEntity>) { ownedItems = items }
        override fun observeOwnedItems(): Flow<List<OwnedItemEntity>> = flowOf(ownedItems)
        override suspend fun deleteAllOwnedItems() { ownedItems = emptyList() }
        override suspend fun upsertEquippedItems(items: List<EquippedItemEntity>) { equippedItems = items }
        override fun observeEquippedItems(): Flow<List<EquippedItemEntity>> = flowOf(equippedItems)
        override suspend fun deleteAllEquippedItems() { equippedItems = emptyList() }
        override suspend fun deleteEquippedItemByType(type: String) { equippedItems = equippedItems.filter { it.type != type } }
        override suspend fun replaceStoreItems(items: List<StoreItemEntity>) { storeItems = items }
        override suspend fun replaceOwnedItems(items: List<OwnedItemEntity>) { ownedItems = items }
        override suspend fun replaceEquippedItems(items: List<EquippedItemEntity>) { equippedItems = items }
        override suspend fun getMinExpiryTime(): Long? = null
        override suspend fun getMinOwnedExpiryTime(): Long? = null
        override suspend fun getMinEquippedExpiryTime(): Long? = null
    }

    private class FakeProfileRepository : ProfileRepository {
        override fun observeProfile(): Flow<com.awan.app.core.domain.profile.model.Profile?> = flowOf(null)
        override suspend fun getProfile(): Result<com.awan.app.core.domain.profile.model.Profile> = Result.Error(AppError.Network)
        override suspend fun updateName(firstName: String, lastName: String) = Result.Error(AppError.Network)
        override suspend fun updateBirthDate(birthDate: String) = Result.Error(AppError.Network)
        override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String) = Result.Error(AppError.Network)
        override suspend fun deleteProfilePicture() = Result.Error(AppError.Network)
        override suspend fun updateProfilePartial(firstName: String?, lastName: String?, timezone: String?, preferredSessionDuration: Int?, bufferBetweenSessions: Int?, wakeupTime: String?, sleepTime: String?, schedulingType: String?) = Result.Error(AppError.Network)
        override suspend fun updateTimezone(timezone: String) = Result.Error(AppError.Network)
        override suspend fun updateSessionSettings(preferredSessionDuration: Int, bufferBetweenSessions: Int) = Result.Error(AppError.Network)
        override suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String) = Result.Error(AppError.Network)
        override suspend fun updateSchedulingType(schedulingType: String) = Result.Error(AppError.Network)
    }

    private class FakeNetworkConnectivityMonitor : NetworkConnectivityMonitor {
        override val isOnline: Flow<Boolean> = MutableStateFlow(true)
        override fun isCurrentlyOnline(): Boolean = true
    }

    private class FakeUserDao : UserDao {
        private var user: UserEntity? = null
        override suspend fun upsertUser(user: UserEntity) { this.user = user }
        override fun observeUser(userId: String): Flow<UserEntity?> = flowOf(user)
        override suspend fun getUser(userId: String): UserEntity? = user
        override suspend fun getFirstUser(): UserEntity? = user
        override suspend fun deleteUser(userId: String) { user = null }
        override suspend fun getMinExpiryTime(): Long? = null
        override suspend fun upsertPreferences(preferences: UserPreferencesEntity) {}
        override fun observePreferences(userId: String): Flow<UserPreferencesEntity?> = flowOf(null)
        override suspend fun getPreferences(userId: String): UserPreferencesEntity? = null
        override fun observeUserWithPreferences(userId: String): Flow<UserWithPreferences?> = flowOf(null)
        override suspend fun getUserWithPreferences(userId: String): UserWithPreferences? = null
        override suspend fun upsertUserWithPreferences(user: UserEntity, preferences: UserPreferencesEntity) { this.user = user }
    }
}
