package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.domain.marketplace.usecase.EquipItemUseCase
import com.awan.app.core.domain.marketplace.usecase.GetEquippedItemsUseCase
import com.awan.app.core.domain.marketplace.usecase.GetInventoryUseCase
import com.awan.app.core.domain.marketplace.usecase.MarkInventorySeenUseCase
import com.awan.app.core.domain.marketplace.usecase.RefreshMarketplaceUseCase
import com.awan.app.core.domain.marketplace.usecase.UnequipItemUseCase
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        var unequipCalls = 0
        var lastUnequippedType: StoreItemType? = null
        var markSeenCalls = 0
        var refreshCalls = 0
        var equipResult: Result<Unit> = Result.Success(Unit)
        var unequipResult: Result<Unit> = Result.Success(Unit)
        var refreshException: Exception? = null

        override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = flowOf(emptyList())
        override fun getInventory(): Flow<List<OwnedItem>> = inventory
        override fun getEquippedItems(): Flow<List<EquippedItem>> = equipped
        override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun equipItem(itemId: String): Result<Unit> {
            equipCalls++
            return equipResult
        }
        override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> {
            unequipCalls++
            lastUnequippedType = itemType
            return unequipResult
        }
        override suspend fun refreshStoreItems(type: StoreItemType?) {
            refreshCalls++
            refreshException?.let { throw it }
        }
        override suspend fun refreshInventory(): Result<Unit> {
            refreshCalls++
            refreshException?.let { throw it }
            return Result.Success(Unit)
        }
        override suspend fun refreshEquippedItems(): Result<Unit> {
            refreshCalls++
            refreshException?.let { throw it }
            return Result.Success(Unit)
        }
        override suspend fun markInventorySeen() {
            markSeenCalls++
        }
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
        unequipItem = UnequipItemUseCase(repository),
        markInventorySeen = MarkInventorySeenUseCase(repository),
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
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun `sort by name orders items alphabetically case-insensitively`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SelectType(StoreItemType.FRAME))
        viewModel.onAction(InventoryAction.SetSort(InventorySort.NAME))

        assertEquals(listOf("Common frame", "Epic frame", "Mystery frame"), viewModel.state.value.sections.single().items.map { it.name })
    }

    @Test
    fun `sort by rarity orders items by rarity rank descending then newest then name`() = runTest(testDispatcher) {
        repository.inventory.value = listOf(
            item("common", "Common frame", "2026-08-01T00:00:00Z", info = "rarity: common"),
            item("epic", "Epic frame", "2026-08-02T00:00:00Z", info = "rarity: epic"),
            item("legendary", "Legendary frame", "2026-08-03T00:00:00Z", info = "rarity: legendary"),
            item("uncommon", "Uncommon frame", "2026-08-01T00:00:00Z", info = "rarity: uncommon"),
            item("rare", "Rare frame", "2026-08-02T00:00:00Z", info = "rarity: rare"),
            item("unknown", "Unknown frame", "2026-08-01T00:00:00Z", info = null),
        )
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SetSort(InventorySort.RARITY))

        val section = viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }
        assertEquals(
            listOf("Legendary frame", "Epic frame", "Rare frame", "Uncommon frame", "Common frame", "Unknown frame"),
            section.items.map { it.name },
        )
    }

    @Test
    fun `select type filter narrows sections to matching item type and resets on null`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SelectType(StoreItemType.SKIN))
        assertEquals(1, viewModel.state.value.sections.size)
        assertEquals(StoreItemType.SKIN, viewModel.state.value.sections.single().type)

        viewModel.onAction(InventoryAction.SelectType(null))
        assertEquals(2, viewModel.state.value.sections.size)
    }

    @Test
    fun `rarity filter toggles and supports multiple selected rarities`() = runTest(testDispatcher) {
        repository.inventory.value = listOf(
            item("common", "Common frame", "2026-08-01T00:00:00Z", info = "rarity: common"),
            item("epic", "Epic frame", "2026-08-02T00:00:00Z", info = "rarity: epic"),
            item("rare", "Rare frame", "2026-08-02T00:00:00Z", info = "rarity: rare"),
            item("skin", "Night skin", "2026-08-03T00:00:00Z", StoreItemType.SKIN, info = "rarity: rare"),
        )
        val viewModel = viewModel()

        // Filter for EPIC
        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.EPIC))
        assertEquals(1, viewModel.state.value.sections.size)
        assertEquals(listOf("Epic frame"), viewModel.state.value.sections.single().items.map { it.name })

        // Add RARE to filter
        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.RARE))
        assertEquals(2, viewModel.state.value.sections.size)
        val frames = viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }
        assertEquals(listOf("Epic frame", "Rare frame"), frames.items.map { it.name })

        // Remove EPIC from filter
        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.EPIC))
        assertEquals(2, viewModel.state.value.sections.size)
        val onlyRareFrames = viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }
        assertEquals(listOf("Rare frame"), onlyRareFrames.items.map { it.name })

        // Remove RARE from filter -> all items shown
        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.RARE))
        assertEquals(2, viewModel.state.value.sections.size)
        assertEquals(3, viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }.items.size)
    }

    @Test
    fun `filter resulting in no matches returns empty sections`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.LEGENDARY))

        assertTrue(viewModel.state.value.sections.isEmpty())
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

        assertNull(viewModel.state.value.equippingItemId)
        assertEquals(1, repository.equipCalls)
    }

    @Test
    fun `equip error populates error in state`() = runTest(testDispatcher) {
        repository.equipResult = Result.Error(AppError.Network)
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.Equip("epic"))

        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.equippingItemId)
    }

    @Test
    fun `unequip is blocked when offline`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        connectivity.online.value = false

        viewModel.onAction(InventoryAction.Unequip(StoreItemType.FRAME))

        assertEquals(0, repository.unequipCalls)
    }

    @Test
    fun `unequip is sent when online and triggers refresh`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val initialRefreshCalls = repository.refreshCalls

        viewModel.onAction(InventoryAction.Unequip(StoreItemType.FRAME))

        assertEquals(1, repository.unequipCalls)
        assertEquals(StoreItemType.FRAME, repository.lastUnequippedType)
        assertTrue(repository.refreshCalls > initialRefreshCalls)
        assertNull(viewModel.state.value.unequippingType)
    }

    @Test
    fun `unequip error populates error in state`() = runTest(testDispatcher) {
        repository.unequipResult = Result.Error(AppError.Server(500))
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.Unequip(StoreItemType.FRAME))

        assertNotNull(viewModel.state.value.error)
        assertNull(viewModel.state.value.unequippingType)
    }

    @Test
    fun `refresh action triggers refreshMarketplace and clears refreshing state`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val initialRefreshCalls = repository.refreshCalls

        viewModel.onAction(InventoryAction.Refresh)

        assertTrue(repository.refreshCalls > initialRefreshCalls)
        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `refresh handles repository exceptions gracefully`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        repository.refreshException = RuntimeException("Network timeout")

        viewModel.onAction(InventoryAction.Refresh)

        assertFalse(viewModel.state.value.isRefreshing)
    }

    @Test
    fun `open and close details updates detailsItemId and detailsItem`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.OpenDetails("epic"))
        assertEquals("epic", viewModel.state.value.detailsItemId)
        assertEquals("epic", viewModel.state.value.detailsItem?.item?.id)
        assertEquals("Epic frame", viewModel.state.value.detailsItem?.item?.name)

        viewModel.onAction(InventoryAction.CloseDetails)
        assertNull(viewModel.state.value.detailsItemId)
        assertNull(viewModel.state.value.detailsItem)
    }

    @Test
    fun `open details with nonexistent id sets detailsItemId but detailsItem is null`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.OpenDetails("nonexistent"))
        assertEquals("nonexistent", viewModel.state.value.detailsItemId)
        assertNull(viewModel.state.value.detailsItem)
    }

    @Test
    fun `unseen items are tracked in unseenItemIds and markSeen clears them`() = runTest(testDispatcher) {
        repository.inventory.value = listOf(
            item("common", "Common frame", "2026-08-01T00:00:00Z", isSeen = true),
            item("epic", "Epic frame", "2026-08-02T00:00:00Z", isSeen = false),
            item("mystery", "Mystery frame", "2026-08-03T00:00:00Z", isSeen = false),
        )
        val viewModel = viewModel()

        assertEquals(setOf("epic", "mystery"), viewModel.state.value.unseenItemIds)

        viewModel.onAction(InventoryAction.MarkSeen)

        assertEquals(1, repository.markSeenCalls)
        assertTrue(viewModel.state.value.unseenItemIds.isEmpty())
    }

    @Test
    fun `equipped items flow updates equippedItemIds in state`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        assertEquals(emptySet<String>(), viewModel.state.value.equippedItemIds)

        val item = repository.inventory.value.first()
        repository.equipped.value = listOf(
            EquippedItem(
                type = StoreItemType.FRAME,
                item = item.item,
                equippedAt = "2026-08-05T00:00:00Z",
            ),
        )

        assertEquals(setOf("common"), viewModel.state.value.equippedItemIds)
        val section = viewModel.state.value.sections.single { it.type == StoreItemType.FRAME }
        val commonItem = section.items.single { it.itemId == "common" }
        assertTrue(commonItem.isEquipped)
    }

    private fun item(
        id: String,
        name: String,
        acquiredAt: String,
        type: StoreItemType = StoreItemType.FRAME,
        info: String? = null,
        isSeen: Boolean = true,
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
            type = type,
        ),
        boughtAt = acquiredAt,
        isSeen = isSeen,
    )
}
