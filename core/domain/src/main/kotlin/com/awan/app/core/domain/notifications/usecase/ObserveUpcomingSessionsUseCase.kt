package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.domain.notifications.model.UpcomingSession
import com.awan.app.core.domain.notifications.repository.SessionNotificationRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveUpcomingSessionsUseCase @Inject constructor(
    private val repository: SessionNotificationRepository,
) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<List<UpcomingSession>> =
        repository.observeUpcoming(startDate, endDate)
}
