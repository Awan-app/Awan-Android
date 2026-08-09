package com.awan.app.core.data.marketplace

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.marketplace.remote.StoreRemoteDataSource
import com.awan.app.core.data.marketplace.repository.StoreRepositoryImpl
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.model.EquippedItemEntity
import com.awan.app.core.database.model.OwnedItemEntity
import com.awan.app.core.database.model.StoreItemEntity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.profile.repository.ProfileRepository
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.network.dto.store.EquippedItemDto
import com.awan.app.core.network.dto.store.OwnedItemDto
import com.awan.app.core.network.dto.store.StoreItemDto
import com.awan.app.core.network.dto.store.StoreItemTypeDto
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StoreRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: StoreRepositoryImpl
    private lateinit var fakeRemoteDataSource: FakeStoreRemoteDataSource
    private lateinit var fakeStoreDao: FakeStoreDao
    private lateinit var fakeProfileRepository: FakeProfileRepository
    private lateinit var fakeConnectivityMonitor: FakeConnectivityMonitor

    @Before
    fun setup() {
        fakeRemoteDataSource = FakeStoreRemoteDataSource()
        fakeStoreDao = FakeStoreDao()
        fakeProfileRepository = FakeProfileRepository()
        fakeConnectivityMonitor = FakeConnectivityMonitor()
        repository = StoreRepositoryImpl(
            remoteDataSource = fakeRemoteDataSource,
            storeDao = fakeStoreDao,
            profileRepository = fakeProfileRepository,
            connectivityMonitor = fakeConnectivityMonitor,
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun `getStoreItems returns items from DAO`() = runTest(testDispatcher) {
        val entities = listOf(
            StoreItemEntity("1", "Item 1", "Desc", "img", null, 100, "1.0", "FRAME")
        )
        fakeStoreDao.storeItems = entities

        val result = repository.getStoreItems(null).first()

        assertEquals(1, result.size)
        assertEquals("1", result[0].id)
        assertEquals(StoreItemType.FRAME, result[0].type)
    }

    @Test
    fun `buyItem calls remote and refreshes inventory and profile`() = runTest(testDispatcher) {
        fakeConnectivityMonitor.online = true
        fakeRemoteDataSource.inventoryResponse = Result.Success(listOf(
            OwnedItemDto("o1", StoreItemDto("1", "Item 1", type = "FRAME"), "now")
        ))

        val result = repository.buyItem("1")

        assertTrue(result is Result.Success<*>)
        assertTrue(fakeRemoteDataSource.buyCalled)
        assertTrue(fakeProfileRepository.getProfileCalled)
        
        val inventory = repository.getInventory().first()
        assertEquals(1, inventory.size)
        assertEquals("o1", inventory[0].id)
    }

    // Fakes
    private class FakeStoreRemoteDataSource : StoreRemoteDataSource {
        var buyCalled = false
        var inventoryResponse: Result<List<OwnedItemDto>> = Result.Success(emptyList())

        override suspend fun getStoreItems(type: StoreItemTypeDto?): Result<List<StoreItemDto>> = Result.Success(emptyList())
        override suspend fun getInventory(): Result<List<OwnedItemDto>> = inventoryResponse
        override suspend fun buyItem(itemId: String): Result<Unit> {
            buyCalled = true
            return Result.Success(Unit)
        }
        override suspend fun getEquippedItems(): Result<List<EquippedItemDto>> = Result.Success(emptyList())
        override suspend fun equipItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun unequipItem(itemId: String): Result<Unit> = Result.Success(Unit)
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
    }

    private class FakeProfileRepository : ProfileRepository {
        var getProfileCalled = false
        override fun observeProfile(): Flow<com.awan.app.core.domain.profile.model.Profile?> = flowOf(null)
        override suspend fun getProfile(): Result<com.awan.app.core.domain.profile.model.Profile> {
            getProfileCalled = true
            return Result.Error(com.awan.app.core.common.error.AppError.Network)
        }
        override suspend fun updateName(firstName: String, lastName: String) = error("")
        override suspend fun updateBirthDate(birthDate: String) = error("")
        override suspend fun updateProfilePicture(imageBytes: ByteArray, mimeType: String) = error("")
        override suspend fun deleteProfilePicture() = error("")
        override suspend fun updateProfilePartial(firstName: String?, lastName: String?, timezone: String?, preferredSessionDuration: Int?, bufferBetweenSessions: Int?, wakeupTime: String?, sleepTime: String?, schedulingType: String?) = error("")
        override suspend fun updateTimezone(timezone: String) = error("")
        override suspend fun updateSessionSettings(preferredSessionDuration: Int, bufferBetweenSessions: Int) = error("")
        override suspend fun updateSleepSchedule(wakeupTime: String, sleepTime: String) = error("")
        override suspend fun updateSchedulingType(schedulingType: String) = error("")
    }

    private class FakeConnectivityMonitor : NetworkConnectivityMonitor {
        var online = true
        override val isOnline: Flow<Boolean> = flowOf(online)
        override fun isCurrentlyOnline(): Boolean = online
    }
}
