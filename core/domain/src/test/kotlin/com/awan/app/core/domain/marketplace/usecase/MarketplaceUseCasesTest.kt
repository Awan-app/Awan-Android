package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.EquippedItem
import com.awan.app.core.model.OwnedItem
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MarketplaceUseCasesTest {

    private lateinit var fakeRepository: FakeStoreRepository
    private lateinit var markInventorySeenUseCase: MarkInventorySeenUseCase
    private lateinit var unequipItemUseCase: UnequipItemUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeStoreRepository()
        markInventorySeenUseCase = MarkInventorySeenUseCase(fakeRepository)
        unequipItemUseCase = UnequipItemUseCase(fakeRepository)
    }

    @Test
    fun `MarkInventorySeenUseCase delegates to repository`() = runTest {
        markInventorySeenUseCase()
        assertTrue(fakeRepository.markInventorySeenCalled)
    }

    @Test
    fun `UnequipItemUseCase delegates to repository with specified type`() = runTest {
        val result = unequipItemUseCase(StoreItemType.THEME)
        assertTrue(result is Result.Success)
        assertEquals(StoreItemType.THEME, fakeRepository.lastUnequippedType)
    }

    private class FakeStoreRepository : StoreRepository {
        var markInventorySeenCalled = false
        var lastUnequippedType: StoreItemType? = null

        override fun getStoreItems(type: StoreItemType?): Flow<List<StoreItem>> = emptyFlow()
        override fun getInventory(): Flow<List<OwnedItem>> = emptyFlow()
        override fun getEquippedItems(): Flow<List<EquippedItem>> = emptyFlow()
        override suspend fun buyItem(itemId: String): Result<Unit> = Result.Success(Unit)
        override suspend fun equipItem(itemId: String): Result<Unit> = Result.Success(Unit)

        override suspend fun unequipItem(itemType: StoreItemType): Result<Unit> {
            lastUnequippedType = itemType
            return Result.Success(Unit)
        }

        override suspend fun refreshStoreItems(type: StoreItemType?) {}
        override suspend fun refreshInventory(): Result<Unit> = Result.Success(Unit)
        override suspend fun refreshEquippedItems(): Result<Unit> = Result.Success(Unit)

        override suspend fun markInventorySeen() {
            markInventorySeenCalled = true
        }
    }
}
