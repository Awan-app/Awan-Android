package com.awan.app.core.model

import java.time.LocalTime

/**
 * A zone window as the backend resolves it for one date — a real UUID from a weekly template or a
 * one-off override. Distinct from [Zone], which is the local onboarding model with fixed string
 * ids and minutes-from-midnight windows; the two are not interchangeable.
 *
 * [category] is what the window is *for*, auto-created server-side from the zone's name. It is the
 * link between a task (which carries a category) and a session (which carries a zone).
 */
data class DayZone(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val colorHex: String? = null,
    val category: Category? = null,
)
