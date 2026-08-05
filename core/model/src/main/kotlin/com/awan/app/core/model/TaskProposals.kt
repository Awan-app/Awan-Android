package com.awan.app.core.model

import java.time.LocalDateTime

/**
 * What Awan proposed from a note or a photo. Nothing here is persisted — accepting a [TaskProposal]
 * means POSTing its [TaskProposal.draft] and [TaskProposal.sessions] as a fresh task.
 */
data class TaskProposals(
    /** The vision model's raw read of an uploaded image, shown so the user can verify it. Null for a typed note. */
    val sourceSummary: String? = null,
    val tasks: List<TaskProposal> = emptyList(),
)

data class TaskProposal(
    val draft: TaskDraft,
    val sessions: List<ProposedSession> = emptyList(),
    /** Why Awan proposed these times, or what kept it from finding one. Never blank when present. */
    val reason: String? = null,
)

/**
 * A single proposed session, [isAiSuggested] tells the two backend channels apart: timing the
 * source itself stated is fixed ([isAiSuggested] = false), Awan's own availability-grounded pick
 * wears a badge in the UI and can be dropped without losing anything the user asked for.
 */
data class ProposedSession(
    val start: LocalDateTime,
    val end: LocalDateTime,
    val zoneId: String? = null,
    val isAiSuggested: Boolean = false,
)

/** A task plus the sessions it should be created with — the shape `createTasksWithSessions` needs. */
data class TaskWithSessionsDraft(
    val task: TaskDraft,
    val sessions: List<SessionDraft> = emptyList(),
)

/** Drops [ProposedSession.isAiSuggested] — a create request has no concept of "who picked this". */
fun ProposedSession.toSessionDraft(): SessionDraft = SessionDraft(start = start, end = end, zoneId = zoneId)
