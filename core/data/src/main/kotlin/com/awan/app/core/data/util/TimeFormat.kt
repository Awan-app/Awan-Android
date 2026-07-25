package com.awan.app.core.data.util

import com.awan.app.core.model.DayBounds
import java.util.Locale

/** Minutes-from-midnight to the `HH:mm:ss` the API expects. Wraps, so 1500 becomes `01:00:00`. */
internal fun formatMinutesToTime(minutes: Int): String {
    val totalMinutes = minutes.mod(DayBounds.MINUTES_PER_DAY)
    return String.format(Locale.US, "%02d:%02d:00", totalMinutes / 60, totalMinutes % 60)
}
