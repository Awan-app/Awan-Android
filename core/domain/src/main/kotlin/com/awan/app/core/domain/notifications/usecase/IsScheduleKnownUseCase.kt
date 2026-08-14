package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.domain.notifications.repository.SessionNotificationRepository
import java.time.LocalDate
import javax.inject.Inject

/**
 * Separates "this day is empty" from "this day has never been fetched", which look the same from the
 * sessions table alone.
 */
class IsScheduleKnownUseCase @Inject constructor(
    private val repository: SessionNotificationRepository,
) {
    suspend operator fun invoke(date: LocalDate): Boolean = repository.isScheduleKnown(date)
}
