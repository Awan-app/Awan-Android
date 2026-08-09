package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.repository.HomeRepository
import javax.inject.Inject

class MoveSessionUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(
        sessionId: String,
        startIso: String,
        endIso: String,
    ): Result<Unit> = homeRepository.moveSession(
        sessionId = sessionId,
        startIso = startIso,
        endIso = endIso,
    )
}
