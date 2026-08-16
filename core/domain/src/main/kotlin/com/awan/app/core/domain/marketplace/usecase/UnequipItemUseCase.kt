package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import com.awan.app.core.model.StoreItemType
import javax.inject.Inject

class UnequipItemUseCase @Inject constructor(
    private val repository: StoreRepository,
) {
    suspend operator fun invoke(type: StoreItemType): Result<Unit> = repository.unequipItem(type)
}
