package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.domain.notifications.repository.SessionNotificationRepository
import java.time.LocalDate
import javax.inject.Inject

class GetUpcomingSessionsUseCase @Inject constructor(
    private val repository: SessionNotificationRepository,
) {
    suspend operator fun invoke(startDate: LocalDate, endDate: LocalDate): List<UpcomingSession> =
        repository.getUpcoming(startDate, endDate)
}
