package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.OwnedItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetInventoryUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(): Flow<List<OwnedItem>> =
        repository.getInventory()
}
