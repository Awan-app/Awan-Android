package com.awan.app.core.domain.marketplace.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.marketplace.repository.StoreRepository
import javax.inject.Inject

class BuyItemUseCase @Inject constructor(
    private val repository: StoreRepository
) {
    suspend operator fun invoke(itemId: String): Result<Unit> =
        repository.buyItem(itemId)
}
