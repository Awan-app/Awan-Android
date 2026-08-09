package com.awan.app.core.domain.inventory.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.inventory.repository.InventoryRepository
import javax.inject.Inject

class EquipCustomizationUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(itemId: String): Result<Unit> = repository.equip(itemId)
}
