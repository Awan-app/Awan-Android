package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.domain.marketplace.repository.StoreRepository
import javax.inject.Inject

class RefreshMarketplaceUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke() {
        repository.refreshStoreItems()
        repository.refreshInventory()
        repository.refreshEquippedItems()
    }
}
