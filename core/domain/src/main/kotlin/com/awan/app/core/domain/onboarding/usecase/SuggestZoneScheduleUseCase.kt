package com.awan.app.core.domain.onboarding.usecase

import com.awan.app.core.domain.onboarding.utils.ZoneEditRules
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.Zone
import javax.inject.Inject

/**
 * Deterministic (NOT AI) zone suggestion: the four fixed default zones, in order, laid contiguously
 * from wake + 30 min to sleep − 30 min as an equal split with each boundary snapped to 5 minutes.
 * The 30-min margins are wake-up / wind-down routine time. Must be byte-identical with the iOS
 * engine for the same input (shared QA test vectors, AWAN-45 DoD).
 */
class SuggestZoneScheduleUseCase @Inject constructor() {

    operator fun invoke(bounds: DayBounds): List<Zone> {
        val defaults = Zone.defaults
        val usable = (bounds.wakingMinutes - 2 * ROUTINE_MARGIN_MINUTES).coerceAtLeast(0)
        val firstBoundary = bounds.wakeMinutes + ROUTINE_MARGIN_MINUTES
        val boundaries = (0..defaults.size).map { i ->
            firstBoundary + ZoneEditRules.snapToFive(usable * i / defaults.size)
        }
        return defaults.mapIndexed { i, zone ->
            val startLinear = boundaries[i]
            val duration = boundaries[i + 1] - boundaries[i]
            val startMinutes = startLinear.mod(DayBounds.MINUTES_PER_DAY)
            zone.copy(startMinutes = startMinutes, endMinutes = startMinutes + duration, isEnabled = true)
        }
    }

    private companion object {
        const val ROUTINE_MARGIN_MINUTES = 30
    }
}
