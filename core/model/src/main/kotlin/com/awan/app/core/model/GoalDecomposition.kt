package com.awan.app.core.model

/**
 * A single reply from the AI goal-decomposition endpoint.
 *
 * [sessionId] must be forwarded as-is to the next call to continue the session.
 * [blocks] are ordered — the caller should render them in array order.
 * [hasProposal] is true when at least one [GoalDecompositionBlock.Proposal] is present.
 */
data class GoalDecompositionReply(
    val sessionId: String,
    val blocks: List<GoalDecompositionBlock>,
    val hasProposal: Boolean,
)

/** Supported block types returned by the AI decomposition endpoint. Unknown types are dropped. */
sealed interface GoalDecompositionBlock {
    /** Plain text from the assistant (e.g., an acknowledgement or summary). */
    data class Text(val text: String) : GoalDecompositionBlock

    /**
     * A question the assistant is asking.
     *
     * When [options] is non-empty the presentation layer renders multiple-choice;
     * when empty (or missing from the response) it renders a free-text writing field.
     */
    data class Question(val text: String, val options: List<String>) : GoalDecompositionBlock

    /** A concrete goal-and-tasks proposal ready for user confirmation. */
    data class Proposal(val proposal: GoalProposal) : GoalDecompositionBlock
}

/** A goal proposal returned by the AI decomposition endpoint. */
data class GoalProposal(
    val title: String,
    val description: String?,
    /** ISO-8601 date string (YYYY-MM-DD) or null if the backend omitted it. */
    val targetDate: String?,
    val tasks: List<ProposedTask>,
)

/** A single task inside a [GoalProposal]. */
data class ProposedTask(
    val title: String,
    /** Estimated duration in minutes. Null if the backend omitted it. */
    val estimatedDuration: Int?,
    /** Gamification points. Null if the backend omitted it. */
    val estimatedPoints: Int?,
)

/** Full transcript of a decomposition session. */
data class GoalDecompositionTranscript(
    val sessionId: String,
    val status: String,
    val messages: List<DecompositionMessage>,
    val hasProposal: Boolean,
    val confirmedGoalId: String?,
    val createdAt: String?,
    val updatedAt: String?,
)

/** A message block in a decomposition transcript. */
data class DecompositionMessage(
    val role: String,
    val blocks: List<GoalDecompositionBlock>,
)

