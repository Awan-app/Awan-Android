package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.Zone

/** A block positioned as fractions [0f,1f] along the waking window, for the live day timeline. */
data class DayBlockUi(
    val id: String,
    val label: String,
    val colorArgb: Int,
    val startFraction: Float,
    val endFraction: Float,
    val enabled: Boolean,
)

data class DayPreviewModel(
    val wakeMinutes: Int,
    val sleepMinutes: Int,
    val zones: List<DayBlockUi>,
    val task: DayBlockUi?,
) {
    companion object {
        fun from(bounds: DayBounds, zones: List<Zone>, firstTask: FirstTask?): DayPreviewModel {
            val waking = bounds.wakingMinutes.coerceAtLeast(1)
            val zoneBlocks = zones.map { zone ->
                val start = (zone.startMinutes - bounds.wakeMinutes).mod(DayBounds.MINUTES_PER_DAY).toFloat() / waking
                DayBlockUi(
                    id = zone.id,
                    label = zone.name,
                    colorArgb = zone.colorArgb,
                    startFraction = start.coerceIn(0f, 1f),
                    endFraction = (start + zone.durationMinutes.toFloat() / waking).coerceIn(0f, 1f),
                    enabled = zone.isEnabled,
                )
            }
            val taskBlock = firstTask?.let {
                val start = (it.startMinutes - bounds.wakeMinutes).mod(DayBounds.MINUTES_PER_DAY).toFloat() / waking
                DayBlockUi(
                    id = it.id,
                    label = it.title,
                    colorArgb = zones.firstOrNull { z -> z.id == it.zoneId }?.colorArgb ?: 0,
                    startFraction = start.coerceIn(0f, 1f),
                    endFraction = (start + it.durationMinutes.toFloat() / waking).coerceIn(0f, 1f),
                    enabled = true,
                )
            }
            return DayPreviewModel(bounds.wakeMinutes, bounds.sleepMinutes, zoneBlocks, taskBlock)
        }
    }
}
