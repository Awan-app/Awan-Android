package com.awan.feature.marketplace.impl.presentation

import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.marketplace.usecase.*
import com.awan.app.core.domain.profile.usecase.ObserveProfileUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
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
class MarketplaceViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var storeRepository: FakeStoreRepository
    private lateinit var profileRepository: FakeProfileRepository
    private lateinit var authTokenProvider: FakeAuthTokenProvider
    private lateinit var viewModel: MarketplaceViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        storeRepository = FakeStoreRepository()
        profileRepository = FakeProfileRepository()
        authTokenProvider = FakeAuthTokenProvider()
        
        val item1 = StoreItem("1", "Frame 1", "Desc", "img", null, 100, "1.0", StoreItemType.FRAME)
        val item2 = StoreItem("2", "Skin 1", "Desc", "img", null, 200, "1.0", StoreItemType.SKIN)
        storeRepository.storeItemsFlow.value = listOf(item1, item2)
        
        profileRepository.profileFlow.value = Profile("u1", "email", "First", "Last", "2000-01-01", 500, 5, 10, null, false, null)
        
        viewModel = MarketplaceViewModel(
            getStoreItemsUseCase = GetStoreItemsUseCase(storeRepository),
            getInventoryUseCase = GetInventoryUseCase(storeRepository),
            getEquippedItemsUseCase = GetEquippedItemsUseCase(storeRepository),
            buyItemUseCase = BuyItemUseCase(storeRepository),
            equipItemUseCase = EquipItemUseCase(storeRepository),
            unequipItemUseCase = UnequipItemUseCase(storeRepository),
            refreshMarketplaceUseCase = RefreshMarketplaceUseCase(storeRepository),
            observeProfileUseCase = ObserveProfileUseCase(profileRepository)
        )
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads items and points on init`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        val state = viewModel.state.value
        assertEquals(2, state.items.size)
        assertEquals(500, state.points)
        assertFalse(state.isLoading)
    }

    @Test
    fun `filters items by category`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(MarketplaceAction.SelectCategory(StoreItemType.FRAME))
        assertEquals(1, viewModel.state.value.filteredItems.size)
        assertEquals(StoreItemType.FRAME, viewModel.state.value.filteredItems.first().type)
    }

    @Test
    fun `searches items by name`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        viewModel.onAction(MarketplaceAction.UpdateSearchQuery("Skin"))
        assertEquals(1, viewModel.state.value.filteredItems.size)
        assertEquals("Skin 1", viewModel.state.value.filteredItems.first().name)
    }

    @Test
    fun `ownership derivation works`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        val item = storeRepository.storeItemsFlow.value.first()
        storeRepository.inventoryFlow.value = listOf(OwnedItem("oi1", item, "now"))
        
        viewModel.onAction(MarketplaceAction.Refresh)
        
        val state = viewModel.state.value
        assertTrue(item.id in state.ownedItemIds)
        assertEquals(1, state.ownedItems.size)
        assertEquals(1, state.lockedItems.size)
    }
    
    @Test
    fun `buys item successfully updates inventory`() = runTest(testDispatcher) {
        backgroundScope.launch { viewModel.state.collect {} }
        val item = storeRepository.storeItemsFlow.value.first()
        
        // Prepare inventory that will be returned after buy
        storeRepository.inventoryFlow.value = listOf(OwnedItem("oi1", item, "now"))
        
        viewModel.onAction(MarketplaceAction.BuyItem(item))
        
        assertTrue(item.id in viewModel.state.value.ownedItemIds)
    }
}

private class FakeAuthTokenProvider : AuthTokenProvider {
    override suspend fun getAccessToken(): String? = "test-token"
    override suspend fun getRefreshToken(): String? = null
    override suspend fun saveTokens(accessToken: String, refreshToken: String) {}
    override suspend fun saveUserData(userId: String?, email: String?) {}
    override suspend fun getUserId(): String? = null
    override suspend fun getUserEmail(): String? = null
    override suspend fun clearTokens() {}
    override fun observeIsLoggedIn(): kotlinx.coroutines.flow.Flow<Boolean> = kotlinx.coroutines.flow.emptyFlow()
    override suspend fun setLoggedIn(loggedIn: Boolean) {}
    override suspend fun saveFcmToken(token: String) {}
    override suspend fun getFcmToken(): String? = null
    override val sessionExpired: kotlinx.coroutines.flow.Flow<Unit> = kotlinx.coroutines.flow.emptyFlow()
    override fun notifySessionExpired() {}
}
