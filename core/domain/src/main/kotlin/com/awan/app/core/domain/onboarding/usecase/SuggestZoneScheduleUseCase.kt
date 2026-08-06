package com.awan.app.core.domain.onboarding.usecase

import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.domain.onboarding.utils.ZoneEditRules
import javax.inject.Inject

/**
 * Deterministic (NOT AI) zone suggestion: the four fixed default zones, in order, laid contiguously
 * from wake + 30 min to sleep − 30 min, each taking its [WEIGHTS] share of the usable window with
 * every boundary snapped to 5 minutes. The 30-min margins are wake-up / wind-down routine time.
 * Must be byte-identical with the iOS engine for the same input (shared QA test vectors, AWAN-45 DoD).
 */
class SuggestZoneScheduleUseCase @Inject constructor() {

    operator fun invoke(bounds: DayBounds): List<Zone> {
        val defaults = Zone.defaults
        val usable = (bounds.wakingMinutes - 2 * ROUTINE_MARGIN_MINUTES).coerceAtLeast(0)
        val firstBoundary = bounds.wakeMinutes + ROUTINE_MARGIN_MINUTES
        val cumulative = defaults.runningFold(0) { sum, zone -> sum + WEIGHTS.getValue(zone.id) }
        val total = cumulative.last()
        val boundaries = cumulative.map { weight ->
            firstBoundary + ZoneEditRules.snapToFive(usable * weight / total)
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

        /**
         * Shares of the usable window, not hours: a 15h day gives Work 6h / Learning 2h / Personal 2h /
         * General 5h, and a shorter day scales all four down rather than starving the last one.
         */
        val WEIGHTS = mapOf(
            Zone.WORK to 6,
            Zone.LEARNING to 2,
            Zone.PERSONAL to 2,
            Zone.GENERAL to 5,
        )
    }
}
