package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.valueobjects.TimeRange
import java.time.Instant

interface AvailabilityCalculating {
    fun freeRanges(
        inside: TimeRange,
        excluding: List<TimeRange>,
        notBefore: Instant?
    ): List<TimeRange>
}

class DefaultAvailabilityCalculator : AvailabilityCalculating {
    override fun freeRanges(
        inside: TimeRange,
        excluding: List<TimeRange>,
        notBefore: Instant?
    ): List<TimeRange> {
        val effectiveStart = maxOf(inside.start, notBefore ?: inside.start)
        if (!effectiveStart.isBefore(inside.end)) return emptyList()

        // Collect and clip occupied ranges to our window
        val relevant = excluding
            .filter { it.end.isAfter(effectiveStart) && it.start.isBefore(inside.end) }
            .map { Pair(maxOf(it.start, effectiveStart), minOf(it.end, inside.end)) }
            .sortedWith(compareBy({ it.first }, { it.second }))

        // Merge overlapping occupied ranges
        val merged = mutableListOf<Pair<Instant, Instant>>()
        for (range in relevant) {
            val last = merged.lastOrNull()
            if (last != null && !range.first.isAfter(last.second)) {
                merged[merged.size - 1] = Pair(last.first, maxOf(last.second, range.second))
            } else {
                merged.add(range)
            }
        }

        // Invert to find free ranges
        val free = mutableListOf<TimeRange>()
        var cursor = effectiveStart
        for ((busyStart, busyEnd) in merged) {
            if (cursor.isBefore(busyStart)) {
                free.add(TimeRange(cursor, busyStart))
            }
            cursor = maxOf(cursor, busyEnd)
        }
        if (cursor.isBefore(inside.end)) {
            free.add(TimeRange(cursor, inside.end))
        }
        return free
    }
}
