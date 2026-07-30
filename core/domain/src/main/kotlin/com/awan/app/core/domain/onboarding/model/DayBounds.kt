package com.awan.app.core.domain.onboarding.model

/**
 * Waking day expressed as minutes-from-midnight. [sleepMinutes] < [wakeMinutes] means the waking
 * window crosses midnight (e.g. wake 22:00, sleep 06:00).
 */
data class DayBounds(
    val wakeMinutes: Int,
    val sleepMinutes: Int,
) {
    val isOvernight: Boolean get() = sleepMinutes < wakeMinutes

    /** Length of the waking window in minutes, correct across midnight. */
    val wakingMinutes: Int get() = if (isOvernight) MINUTES_PER_DAY - wakeMinutes + sleepMinutes else sleepMinutes - wakeMinutes

    companion object {
        const val MINUTES_PER_DAY = 24 * 60
        val Default = DayBounds(wakeMinutes = 7 * 60, sleepMinutes = 23 * 60)
    }
}
