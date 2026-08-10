package com.awan.app.core.domain.zones.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.model.Session
import com.awan.app.core.domain.zones.repository.SessionRepository
import java.time.LocalDate
import javax.inject.Inject

class GetSessionsByDateUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(date: LocalDate): Result<List<Session>> =
        sessionRepository.getSessionsByDate(date)
}
