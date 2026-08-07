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
}

fun TaskWithSessions.deriveDisplayStatus(): InboxTaskDisplayStatus {
    if (sessions.isEmpty()) return InboxTaskDisplayStatus.Drafted
    val nonCancelled = sessions.filter { it.status != SessionStatus.CANCELLED }
    if (nonCancelled.isEmpty()) return InboxTaskDisplayStatus.Cancelled
    if (nonCancelled.all { it.status == SessionStatus.COMPLETED }) return InboxTaskDisplayStatus.Completed
    return InboxTaskDisplayStatus.Active
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
    val timeRange: String,
    /** Persisted status (Scheduled / Completed / Cancelled). */
    val statusLabel: String,
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
) {
    /** Tasks visible after applying search and filter. */
    val visibleTasks: List<InboxTaskUiModel>
        get() {
            var result = allTasks

            // Status filter
            if (activeStatusFilters.isNotEmpty()) {
                result = result.filter { it.displayStatus in activeStatusFilters }
            }

            // Session-display filter — keep tasks that have at least one matching session
            if (activeSessionFilters.isNotEmpty()) {
                result = result.filter { task ->
                    task.sessions.any { session ->
                        (InboxSessionFilter.ActiveNow in activeSessionFilters && session.isActiveNow) ||
                            (InboxSessionFilter.Missed in activeSessionFilters && session.isMissed)
                    }
                }
            }

            // Search
            val q = searchQuery.trim()
            if (q.isNotEmpty()) {
                val lower = q.lowercase()
                result = result.filter { task ->
                    task.title.lowercase().contains(lower) ||
                        task.description?.lowercase()?.contains(lower) == true ||
                        task.sessions.any { s ->
                            s.dateLabel.lowercase().contains(lower) ||
                                s.timeRange.lowercase().contains(lower) ||
                                s.statusLabel.lowercase().contains(lower)
                        }
                }
            }

            return result
        }
}

sealed interface InboxAction {
    data class SearchQueryChanged(val query: String) : InboxAction
    data class StatusFilterToggled(val filter: InboxTaskDisplayStatus) : InboxAction
    data class SessionFilterToggled(val filter: InboxSessionFilter) : InboxAction
    data class TaskExpandToggled(val taskId: String) : InboxAction
    data object RetryClicked : InboxAction
}
