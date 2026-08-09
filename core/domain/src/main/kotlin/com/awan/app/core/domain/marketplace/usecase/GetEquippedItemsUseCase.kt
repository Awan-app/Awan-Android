package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.EquippedItem
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEquippedItemsUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(): Flow<List<EquippedItem>> =
        repository.getEquippedItems()
}
