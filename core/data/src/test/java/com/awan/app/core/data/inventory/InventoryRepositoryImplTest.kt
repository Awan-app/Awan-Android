package com.awan.app.core.data.inventory

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.error.AppError
import com.awan.app.core.data.inventory.remote.InventoryRemoteDataSource
import com.awan.app.core.database.dao.OwnedCustomizationDao
import com.awan.app.core.database.model.OwnedCustomizationEntity
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.network.dto.inventory.EquippedItemResponse
import com.awan.app.core.network.dto.inventory.InventoryItemResponse
import com.awan.app.core.network.dto.inventory.StoreItemResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryRepositoryImplTest {

    private val frame = StoreItemResponse(
        id = "frame-1",
        name = "Aurora Frame",
        description = "A glowing frame",
        image = "https://example.com/aurora.png",
        info = "Rarity: epic",
        price = 200,
        version = "1.0",
        type = "FRAME",
    )
    private val remote = FakeInventoryRemoteDataSource(
        inventory = listOf(InventoryItemResponse(id = "owned-1", item = frame, boughtAt = "2026-08-01T10:00:00Z")),
        equipped = listOf(EquippedItemResponse(type = "FRAME", item = frame, equippedAt = "2026-08-02T10:00:00Z")),
    )
    private val dao = FakeOwnedCustomizationDao()
    private val auth = FakeAuthTokenProvider()
    private val repository = repository(remote, auth)

    @Test
    fun `refresh stores the server equipped item with its derived rarity`() = runTest {
        val result = repository.refresh()

        assertTrue(result is Result.Success<*>)
        assertEquals(
            listOf(
                OwnedCustomizationEntity(
                    userId = "user-1",
                    inventoryId = "owned-1",
                    itemId = "frame-1",
                    name = "Aurora Frame",
                    description = "A glowing frame",
                    imageUrl = "https://example.com/aurora.png",
                    type = "FRAME",
                    rarity = CustomizationRarity.EPIC.name,
                    acquiredAt = "2026-08-01T10:00:00Z",
                    isEquipped = true,
                ),
            ),
            dao.latest,
        )
    }

    @Test
    fun `equip switches the active item using the server slot`() = runTest {
        repository.equip("frame-1")

        assertEquals("user-1" to "FRAME" to "frame-1", dao.lastEquipped)
    }

    @Test
    fun `equip failure never updates the local active slot`() = runTest {
        remote.equipResult = Result.Error(AppError.Network)

        val result = repository.equip("frame-1")

        assertTrue(result is Result.Error)
        assertEquals(null, dao.lastEquipped)
    }

    @Test
    fun `refresh does not write a response to a different account`() = runTest {
        remote.onInventory = { auth.userId = "user-2" }

        val result = repository.refresh()

        assertTrue(result is Result.Error)
        assertEquals(emptyList<OwnedCustomizationEntity>(), dao.latest)
    }

    @Test
    fun `equip does not update a different account after the server response`() = runTest {
        remote.onEquip = { auth.userId = "user-2" }

        val result = repository.equip("frame-1")

        assertTrue(result is Result.Error)
        assertEquals(null, dao.lastEquipped)
    }

    private fun repository(
        remote: FakeInventoryRemoteDataSource,
        auth: FakeAuthTokenProvider,
    ) = InventoryRepositoryImpl(
        remoteDataSource = remote,
        ownedCustomizationDao = dao,
        authTokenProvider = auth,
        connectivityMonitor = FakeNetworkConnectivityMonitor(),
        ioDispatcher = UnconfinedTestDispatcher(),
    )
}

private class FakeInventoryRemoteDataSource(
    private val inventory: List<InventoryItemResponse>,
    private val equipped: List<EquippedItemResponse>,
) : InventoryRemoteDataSource {
    var equipResult: Result<EquippedItemResponse> = Result.Success(equipped.single())
    var onInventory: (() -> Unit)? = null
    var onEquip: (() -> Unit)? = null

    override suspend fun getInventory(): Result<List<InventoryItemResponse>> {
        onInventory?.invoke()
        return Result.Success(inventory)
    }

    override suspend fun getEquipped(): Result<List<EquippedItemResponse>> = Result.Success(equipped)

    override suspend fun equip(itemId: String): Result<EquippedItemResponse> {
        onEquip?.invoke()
        return equipResult
    }
}

private class FakeOwnedCustomizationDao : OwnedCustomizationDao {
    override fun observeForUser(userId: String): Flow<List<OwnedCustomizationEntity>> = MutableStateFlow(emptyList())

    var latest: List<OwnedCustomizationEntity> = emptyList()
    var lastEquipped: Pair<Pair<String, String>, String>? = null

    override suspend fun upsertAll(customizations: List<OwnedCustomizationEntity>) = Unit

    override suspend fun deleteForUser(userId: String) = Unit

    override suspend fun replaceForUser(userId: String, customizations: List<OwnedCustomizationEntity>) {
        latest = customizations
    }

    override suspend fun setEquipped(userId: String, type: String, itemId: String) {
        lastEquipped = (userId to type) to itemId
    }
}

private class FakeAuthTokenProvider : AuthTokenProvider {
    var userId: String? = "user-1"

    override suspend fun getAccessToken(): String? = null
    override suspend fun getRefreshToken(): String? = null
    override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
    override suspend fun saveUserData(userId: String?, email: String?) = Unit
    override suspend fun getUserId(): String? = userId
    override suspend fun getUserEmail(): String? = null
    override suspend fun clearTokens() = Unit
    override fun observeIsLoggedIn(): Flow<Boolean> = MutableStateFlow(true)
    override suspend fun setLoggedIn(loggedIn: Boolean) = Unit
    override val sessionExpired: Flow<Unit> = emptyFlow()
    override fun notifySessionExpired() = Unit
}

private class FakeNetworkConnectivityMonitor : NetworkConnectivityMonitor {
    override val isOnline: Flow<Boolean> = MutableStateFlow(true)
    override fun isCurrentlyOnline(): Boolean = true
}
