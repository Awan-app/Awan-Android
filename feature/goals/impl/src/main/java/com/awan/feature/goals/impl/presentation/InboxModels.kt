package com.awan.feature.goals.impl.presentation

import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.TaskWithSessions
import java.time.LocalDateTime

// ─── Derived task display status ─────────────────────────────────────────────

/**
 * A display-only classification derived from a [TaskWithSessions].
 * Never persisted — computed fresh from the live session list.
 */
enum class InboxTaskDisplayStatus {
    /** No sessions attached yet. */
    Drafted,

    /** At least one SCHEDULED session exists. */
    Active,

    /** All non-CANCELLED sessions are COMPLETED and at least one is COMPLETED. */
    Completed,

    /** Every session is CANCELLED. */
    Cancelled,

    /** Sessions exist, but none are SCHEDULED, and they aren't all COMPLETED or CANCELLED (e.g. they are MISSED). */
    Missed,
}

fun TaskWithSessions.deriveDisplayStatus(): InboxTaskDisplayStatus {
    if (sessions.isEmpty()) return InboxTaskDisplayStatus.Drafted
    val nonCancelled = sessions.filter { it.status != SessionStatus.CANCELLED }
    if (nonCancelled.isEmpty()) return InboxTaskDisplayStatus.Cancelled
    if (nonCancelled.all { it.status == SessionStatus.COMPLETED }) return InboxTaskDisplayStatus.Completed
    if (sessions.any { it.status == SessionStatus.SCHEDULED }) return InboxTaskDisplayStatus.Active
    return InboxTaskDisplayStatus.Missed
}

// ─── Session display filter ───────────────────────────────────────────────────

/**
 * Display-only sub-filter applied on top of [InboxTaskDisplayStatus.Active].
 * These are derived from SCHEDULED sessions; the actual [SessionStatus] is never changed.
 */
enum class InboxSessionFilter {
    /** A SCHEDULED session whose interval contains [LocalDateTime.now]. */
    ActiveNow,

    /** A SCHEDULED session whose end is before [LocalDateTime.now]. */
    Missed,
}

// ─── UI model ─────────────────────────────────────────────────────────────────

data class InboxSessionUiModel(
    val id: String,
    val dateLabel: String,
    val startTime: String,
    val endTime: String,
    /** Persisted status (Scheduled / Completed / Cancelled). */
    val statusLabelRes: Int,
    /** True when this session is "Active now" (SCHEDULED and current time inside window). */
    val isActiveNow: Boolean,
    /** True when this session is "Missed" (SCHEDULED but window already passed). */
    val isMissed: Boolean,
)

data class InboxTaskUiModel(
    val id: String,
    val title: String,
    val description: String?,
    val displayStatus: InboxTaskDisplayStatus,
    val sessions: List<InboxSessionUiModel>,
)
