package com.awan.app.core.model

/** AI Goal Schedule Proposal. */
data class GoalScheduleProposal(
    val goalId: String,
    val proposedSessions: List<ProposedGoalSession>,
    val suggestions: List<GoalScheduleSuggestion> = emptyList(),
    val unscheduledTasks: List<UnscheduledTask> = emptyList(),
)

data class ProposedGoalSession(
    val taskId: String,
    val taskTitle: String? = null,
    val zoneId: String?,
    val start: String,
    val end: String,
    val isSelected: Boolean = true,
)

data class GoalScheduleSuggestion(
    val taskId: String,
    val taskTitle: String? = null,
    val zoneId: String?,
    val start: String,
    val end: String,
    val suggestionType: String,
    val reason: String,
    val overlapInfo: ScheduleOverlapInfo?,
    val isSelected: Boolean = false,
)

data class ScheduleOverlapInfo(
    val taskTitle: String,
    val start: String,
    val end: String,
    val mandatory: Boolean,
    val points: Int,
)

data class UnscheduledTask(
    val taskId: String,
    val taskTitle: String,
    val message: String,
)

data class ConfirmedGoalSession(
    val id: String,
    val taskId: String,
    val zoneId: String?,
    val start: String,
    val end: String,
)


enum class ScheduleDraftState {
    AWAITING_PROPOSAL,
    READY,
    CONFIRMING
}

data class GoalScheduleDraft(
    val goalId: String,
    val state: ScheduleDraftState,
    val proposal: GoalScheduleProposal?
)
