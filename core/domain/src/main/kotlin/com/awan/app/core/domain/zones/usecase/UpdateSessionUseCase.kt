package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.repository.SessionRepository
import com.awan.app.core.model.UpdateSessionParams
import javax.inject.Inject

class UpdateSessionUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(
        sessionId: String,
        params: UpdateSessionParams
    ): Result<Session> =
        sessionRepository.updateSession(sessionId, params)
}
