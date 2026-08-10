package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.repository.ZonesRepository
import javax.inject.Inject

class RefreshZonesUseCase @Inject constructor(
    private val zonesRepository: ZonesRepository,
) {
    suspend operator fun invoke(): Result<Unit> = zonesRepository.refreshZones()
}
