package com.awan.app.core.model

import java.time.LocalTime

/**
 * A zone window as the backend resolves it for one date — a real UUID from a weekly template or a
 * one-off override. Distinct from [Zone], which is the local onboarding model with fixed string
 * ids and minutes-from-midnight windows; the two are not interchangeable.
 */
data class DayZone(
    val id: String,
    val name: String,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val colorHex: String? = null,
)
