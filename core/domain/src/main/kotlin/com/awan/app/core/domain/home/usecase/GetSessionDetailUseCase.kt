package com.awan.app.core.domain.home.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.home.repository.HomeRepository
import com.awan.app.core.model.SessionTaskDetail
import javax.inject.Inject

class GetSessionDetailUseCase @Inject constructor(
    private val homeRepository: HomeRepository,
) {
    suspend operator fun invoke(sessionId: String): Result<SessionTaskDetail> =
        homeRepository.getSessionDetail(sessionId)
}
