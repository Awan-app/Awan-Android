package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.StoreItem
import com.awan.app.core.model.StoreItemType
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStoreItemsUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    operator fun invoke(type: StoreItemType? = null): Flow<List<StoreItem>> =
        repository.getStoreItems(type)
}
