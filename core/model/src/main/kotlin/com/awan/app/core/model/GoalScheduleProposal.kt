package com.awan.app.core.model

/** AI Goal Schedule Proposal. */
data class GoalScheduleProposal(
    val goalId: String,
    val proposedSessions: List<ProposedGoalSession>,
    val reason: String?,
)

data class ProposedGoalSession(
    val taskId: String,
    val zoneId: String?,
    val start: String,
    val end: String,
)
