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

// ─── State & Actions ──────────────────────────────────────────────────────────

data class InboxUiState(
    val isLoading: Boolean = true,
    val isError: Boolean = false,
    /** All inbox tasks fetched from the server — unfiltered. */
    val allTasks: List<InboxTaskUiModel> = emptyList(),
    val searchQuery: String = "",
    /** Active task-status filter chips. Empty = show all. */
    val activeStatusFilters: Set<InboxTaskDisplayStatus> = emptySet(),
    /** Active session-display filter chips. Empty = show all. */
    val activeSessionFilters: Set<InboxSessionFilter> = emptySet(),
    /** The id of the task card currently expanded to show sessions. */
    val expandedTaskId: String? = null,
    /** Tasks visible after applying search and filter. */
    val visibleTasks: List<InboxTaskUiModel> = emptyList(),
    val showFilterSheet: Boolean = false,
)

sealed interface InboxAction {
    data class SearchQueryChanged(val query: String) : InboxAction
    data class StatusFilterToggled(val filter: InboxTaskDisplayStatus) : InboxAction
    data class SessionFilterToggled(val filter: InboxSessionFilter) : InboxAction
    data class TaskExpandToggled(val taskId: String) : InboxAction
    data object FilterClicked : InboxAction
    data object FilterDismissed : InboxAction
    data object RetryClicked : InboxAction
}
