package com.awan.app.core.domain.inventory.usecase

import com.awan.app.core.domain.inventory.repository.InventoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveEquippedFrameUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke(): Flow<String?> = repository.observeEquippedFrame()
}
