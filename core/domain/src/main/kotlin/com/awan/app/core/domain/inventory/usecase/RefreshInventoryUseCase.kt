package com.awan.app.core.domain.inventory.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.inventory.repository.InventoryRepository
import javax.inject.Inject

class RefreshInventoryUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refresh()
}
