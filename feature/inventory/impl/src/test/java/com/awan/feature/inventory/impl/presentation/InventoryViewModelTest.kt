package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.marketplace.usecase.EquipItemUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.GetInventoryUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.network.usecase.ObserveNetworkConnectivityUseCase
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InventoryViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private class FakeStoreRepository : StoreRepository {
        val inventory = MutableStateFlow<List<OwnedItem>>(emptyList())
        val equipped = MutableStateFlow<List<EquippedItem>>(emptyList())
        var equipCalls = 0

        override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = flowOf(emptyList())
        override fun getInventory(): Flow<List<OwnedItem>> = inventory
        override fun getEquippedItems(): Flow<List<EquippedItem>> = equipped
        override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun equipItem(itemId: String): Result<Unit> {
            equipCalls++
            return Result.Success(Unit)
        }
        override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshStoreItems(type: StoreItemType?) {}
        override suspend fun refreshInventory(): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshEquippedItems(): Result<Unit> = Result.Success(Unit)
    }

    private class FakeConnectivityMonitor(isOnline: Boolean) : NetworkConnectivityMonitor {
        val online = MutableStateFlow(isOnline)
        override val isOnline: Flow<Boolean> = online
        override fun isCurrentlyOnline(): Boolean = online.value
    }

    private lateinit var repository: FakeStoreRepository
    private lateinit var connectivity: FakeConnectivityMonitor

    private fun viewModel() = InventoryViewModel(
        getInventory = GetInventoryUseCase(repository),
        getEquippedItems = GetEquippedItemsUseCase(repository),
        refreshMarketplace = RefreshMarketplaceUseCase(repository),
        equipItem = EquipItemUseCase(repository),
        observeConnectivity = ObserveNetworkConnectivityUseCase(connectivity),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeStoreRepository()
        repository.inventory.value = listOf(
            item("common", "Common frame", "2026-08-01T00:00:00Z"),
            item("epic", "Epic frame", "2026-08-02T00:00:00Z"),
            item("mystery", "Mystery frame", "2026-08-03T00:00:00Z"),
            item("skin", "Night skin", "2026-08-03T00:00:00Z", StoreItemType.SKIN),
        )
        connectivity = FakeConnectivityMonitor(isOnline = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `groups by type and sorts by newest by default`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        val frames = viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }

        // Sorted by newest (acquiredAt descending)
        assertEquals(listOf("Mystery frame", "Epic frame", "Common frame"), frames.items.map { it.name })
        assertEquals(2, viewModel.state.value.sections.size)
    }

    @Test
    fun `type and sort options narrow and order the visible items`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SelectType(StoreItemType.FRAME))
        viewModel.onAction(InventoryAction.SetSort(InventorySort.NAME))

        assertEquals(listOf("Common frame", "Epic frame", "Mystery frame"), viewModel.state.value.sections.single().items.map { it.name })
    }

    @Test
    fun `equip is blocked when offline`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        connectivity.online.value = false

        viewModel.onAction(InventoryAction.Equip("epic"))

        assertFalse(viewModel.state.value.isOnline)
        assertEquals(0, repository.equipCalls)
    }

    @Test
    fun `equip is sent when online`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.Equip("epic"))

        assertTrue(viewModel.state.value.equippingItemId == null)
        assertEquals(1, repository.equipCalls)
    }

    @Test
    fun `rarity filter toggles and narrows visible items`() = runTest(testDispatcher) {
        repository.inventory.value = listOf(
            item("common", "Common frame", "2026-08-01T00:00:00Z", info = "rarity: common"),
            item("epic", "Epic frame", "2026-08-02T00:00:00Z", info = "rarity: epic"),
            item("skin", "Night skin", "2026-08-03T00:00:00Z", StoreItemType.SKIN, info = "rarity: rare"),
        )
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.ToggleRarity(com.awan.app.core.domain.inventory.model.CustomizationRarity.EPIC))

        val section = viewModel.state.value.sections.single()
        assertEquals(listOf("Epic frame"), section.items.map { it.name })

        // Toggle again to remove filter
        viewModel.onAction(InventoryAction.ToggleRarity(com.awan.app.core.domain.inventory.model.CustomizationRarity.EPIC))
        assertEquals(2, viewModel.state.value.sections.size)
    }

    private fun item(
        id: String,
        name: String,
        acquiredAt: String,
        type: StoreItemType = StoreItemType.FRAME,
        info: String? = null,
    ) = OwnedItem(
        id = "inventory-$id",
        item = StoreItem(
            id = id,
            name = name,
            description = "",
            image = "",
            info = info,
            price = 0,
            version = "",
            type = type
        ),
        boughtAt = acquiredAt
    )
}
