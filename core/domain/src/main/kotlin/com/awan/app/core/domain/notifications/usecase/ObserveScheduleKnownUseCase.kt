package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.domain.notifications.repository.SessionNotificationRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * [IsScheduleKnownUseCase] as a flow, for the collector that keeps the alarm chain in step.
 *
 * The day notifications are gated on this, and on an empty day the first sync flips it without
 * touching the sessions table — so without watching it, nothing re-plans and the gate stays shut.
 */
class ObserveScheduleKnownUseCase @Inject constructor(
    private val repository: SessionNotificationRepository,
) {
    operator fun invoke(date: LocalDate): Flow<Boolean> = repository.observeScheduleKnown(date)
}
