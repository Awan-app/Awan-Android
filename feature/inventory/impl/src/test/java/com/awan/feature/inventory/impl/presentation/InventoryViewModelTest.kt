package com.awan.feature.inventory.impl.presentation

import com.awan.app.core.common.result.Result
import com.awan.app.core.common.error.AppError
import com.awan.app.core.domain.inventory.model.CustomizationRarity
import com.awan.app.core.domain.inventory.model.CustomizationType
import com.awan.app.core.domain.inventory.model.OwnedCustomization
import com.awan.app.core.domain.inventory.repository.InventoryRepository
import com.awan.app.core.domain.inventory.usecase.EquipCustomizationUseCase
import com.awan.app.core.domain.inventory.usecase.ObserveInventoryUseCase
import com.awan.app.core.domain.inventory.usecase.RefreshInventoryUseCase
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import com.awan.app.core.domain.network.usecase.ObserveNetworkConnectivityUseCase
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

    private class FakeInventoryRepository(items: List<OwnedCustomization>) : InventoryRepository {
        val inventory = MutableStateFlow(items)
        var equipCalls = 0
        var refreshResult: Result<Unit> = Result.Success(Unit)

        override fun observeInventory(): Flow<List<OwnedCustomization>> = inventory

        override fun observeEquippedFrame(): Flow<String?> = flowOf(null)

        override suspend fun refresh(): Result<Unit> = refreshResult

        override suspend fun equip(itemId: String): Result<Unit> {
            equipCalls++
            return Result.Success(Unit)
        }
    }

    private class FakeConnectivityMonitor(isOnline: Boolean) : NetworkConnectivityMonitor {
        val online = MutableStateFlow(isOnline)

        override val isOnline: Flow<Boolean> = online

        override fun isCurrentlyOnline(): Boolean = online.value
    }

    private lateinit var repository: FakeInventoryRepository
    private lateinit var connectivity: FakeConnectivityMonitor

    private fun viewModel() = InventoryViewModel(
        observeInventory = ObserveInventoryUseCase(repository),
        refreshInventory = RefreshInventoryUseCase(repository),
        equipCustomization = EquipCustomizationUseCase(repository),
        observeConnectivity = ObserveNetworkConnectivityUseCase(connectivity),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeInventoryRepository(
            listOf(
                item("common", "Common frame", CustomizationRarity.COMMON, "2026-08-01T00:00:00Z"),
                item("epic", "Epic frame", CustomizationRarity.EPIC, "2026-08-02T00:00:00Z"),
                item("unknown", "Mystery frame", CustomizationRarity.UNKNOWN, "2026-08-03T00:00:00Z"),
                item("skin", "Night skin", CustomizationRarity.RARE, "2026-08-03T00:00:00Z", CustomizationType.SKIN),
            ),
        )
        connectivity = FakeConnectivityMonitor(isOnline = true)
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `groups by type and sorts known rarities before unknown`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        val frames = viewModel.state.value.sections.single { it.type == CustomizationType.FRAME }

        assertEquals(listOf("Epic frame", "Common frame", "Mystery frame"), frames.items.map { it.name })
        assertEquals(2, viewModel.state.value.sections.size)
    }

    @Test
    fun `type rarity and name options narrow and order the visible items`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SelectType(CustomizationType.FRAME))
        viewModel.onAction(InventoryAction.ToggleRarity(CustomizationRarity.COMMON))
        viewModel.onAction(InventoryAction.SetSort(InventorySort.NAME))

        assertEquals(listOf("Common frame"), viewModel.state.value.sections.single().items.map { it.name })
    }

    @Test
    fun `unknown rarities stay after known rarities for newest sorting`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        viewModel.onAction(InventoryAction.SetSort(InventorySort.NEWEST))

        assertEquals(
            listOf("Epic frame", "Common frame", "Mystery frame"),
            viewModel.state.value.sections.single { it.type == CustomizationType.FRAME }.items.map { it.name },
        )
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
    fun `refresh failure stops loading and exposes the error`() = runTest(testDispatcher) {
        repository.refreshResult = Result.Error(AppError.Network)

        val viewModel = viewModel()

        assertFalse(viewModel.state.value.isRefreshing)
        assertFalse(viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)
    }

    private fun item(
        id: String,
        name: String,
        rarity: CustomizationRarity,
        acquiredAt: String,
        type: CustomizationType = CustomizationType.FRAME,
    ) = OwnedCustomization(
        inventoryId = "inventory-$id",
        itemId = id,
        name = name,
        description = "",
        imageUrl = null,
        type = type,
        rarity = rarity,
        acquiredAt = acquiredAt,
        isEquipped = false,
    )
}
