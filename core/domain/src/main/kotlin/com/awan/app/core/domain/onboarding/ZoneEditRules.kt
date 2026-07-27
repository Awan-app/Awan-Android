package com.awan.app.core.domain.onboarding

import com.awan.app.core.domain.onboarding.model.DayBounds
import com.awan.app.core.domain.zones.model.Zone

/**
 * Pure editing rules for the zones step. All math is done in "minutes since wake" so it is correct
 * across midnight; results are converted back to minutes-from-midnight on the [Zone].
 */
object ZoneEditRules {

    const val SNAP_MINUTES = 5
    const val MIN_ZONE_MINUTES = 15

    fun snapToFive(minutes: Int): Int = ((minutes + SNAP_MINUTES / 2) / SNAP_MINUTES) * SNAP_MINUTES

    private fun linear(absoluteMinute: Int, wakeMinutes: Int): Int = (absoluteMinute - wakeMinutes).mod(DayBounds.MINUTES_PER_DAY)

    private fun absolute(linearMinute: Int, wakeMinutes: Int): Int = (linearMinute + wakeMinutes).mod(DayBounds.MINUTES_PER_DAY)

    /** Linear [start, end) of a zone in the waking timeline; end may exceed the day length when it wraps. */
    private fun span(zone: Zone, wakeMinutes: Int): IntRange {
        val start = linear(zone.startMinutes, wakeMinutes)
        return start until start + zone.durationMinutes
    }

    /**
     * Apply a drag edit to one zone: snap both edges to 5, enforce the 15-min minimum, and clamp
     * inside the waking window [wake+0 .. sleep]. Non-destructive to sibling zones (overlaps are
     * flagged elsewhere, not prevented).
     *
     * A user-set window never crosses midnight: the backend stores zone edges as `LocalTime` and
     * rejects an end that is not after its start, so the window is cut short at the end of the day.
     */
    fun editWindow(zone: Zone, newStartMinutes: Int, newEndMinutes: Int, bounds: DayBounds): Zone {
        val waking = bounds.wakingMinutes
        var linStart = linear(snapToFive(newStartMinutes), bounds.wakeMinutes).coerceIn(0, waking - MIN_ZONE_MINUTES)
        var linEnd = linStart + (snapToFive(newEndMinutes) - snapToFive(newStartMinutes))
        linEnd = linEnd.coerceIn(linStart + MIN_ZONE_MINUTES, waking)
        if (linEnd - linStart < MIN_ZONE_MINUTES) linStart = (linEnd - MIN_ZONE_MINUTES).coerceAtLeast(0)
        val duration = linEnd - linStart
        val absStart = absolute(linStart, bounds.wakeMinutes)
            .coerceAtMost(DayBounds.MINUTES_PER_DAY - MIN_ZONE_MINUTES)
        val absEnd = (absStart + duration).coerceAtMost(DayBounds.MINUTES_PER_DAY)
        return zone.copy(startMinutes = absStart, endMinutes = absEnd)
    }

    /**
     * Re-lay the zones back-to-back in list order. Each zone keeps its own duration; the chain is
     * anchored at the earliest start currently occupied, so the wake-up routine margin survives a
     * reorder. Disabled zones stay in the chain — they still own their slot in the day.
     */
    fun resequence(zones: List<Zone>, bounds: DayBounds): List<Zone> {
        var cursor = zones.minOfOrNull { linear(it.startMinutes, bounds.wakeMinutes) } ?: return zones
        return zones.map { zone ->
            val start = absolute(cursor, bounds.wakeMinutes)
            cursor += zone.durationMinutes
            zone.copy(startMinutes = start, endMinutes = start + zone.durationMinutes)
        }
    }

    /** IDs of enabled zones that overlap at least one other enabled zone. Non-blocking flag only. */
    fun overlappingZoneIds(zones: List<Zone>, bounds: DayBounds): Set<String> {
        val enabled = zones.filter { it.isEnabled }
        val spans = enabled.associate { it.id to span(it, bounds.wakeMinutes) }
        val flagged = mutableSetOf<String>()
        for (i in enabled.indices) {
            for (j in i + 1 until enabled.size) {
                val a = spans.getValue(enabled[i].id)
                val b = spans.getValue(enabled[j].id)
                if (a.first < b.last + 1 && b.first < a.last + 1) {
                    flagged += enabled[i].id
                    flagged += enabled[j].id
                }
            }
        }
        return flagged
    }
}
