package com.awan.app.core.data.goal

import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalDecompositionReply
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.ProposedTask
import com.awan.app.core.network.dto.GoalDecomposeBlockDto
import com.awan.app.core.network.dto.GoalDecomposeResponse

/**
 * Maps a [GoalDecomposeResponse] to the domain [GoalDecompositionReply].
 *
 * Ordering of [GoalDecompositionReply.blocks] matches the server-returned array order.
 * Unknown or invalid block types are silently ignored (no crash).
 */
internal fun GoalDecomposeResponse.toDecompositionReply(): GoalDecompositionReply =
    GoalDecompositionReply(
        sessionId = sessionId,
        blocks = blocks.mapNotNull { it.toBlock() },
        hasProposal = hasProposal,
    )

/**
 * Converts a raw [GoalDecomposeBlockDto] to a typed [GoalDecompositionBlock], or null if the
 * block type is unknown or required fields are missing (e.g. a "proposal" block without a
 * proposal sub-object, or a proposal without a title).
 */
private fun GoalDecomposeBlockDto.toBlock(): GoalDecompositionBlock? = when (type) {
    "text" -> {
        // text is technically required for a text block; skip if absent
        val t = text ?: return null
        GoalDecompositionBlock.Text(t)
    }

    "question" -> {
        val q = text ?: return null
        // Missing or null options → empty list (writing-question candidate)
        GoalDecompositionBlock.Question(text = q, options = options.orEmpty())
    }

    "proposal" -> {
        val dto = proposal ?: return null
        val title = dto.title ?: return null
        GoalDecompositionBlock.Proposal(
            proposal = GoalProposal(
                title = title,
                description = dto.description,
                // Retain as a validated nullable string; keep domain transport-free
                targetDate = dto.targetDate,
                tasks = dto.tasks.map { task ->
                    ProposedTask(
                        title = task.title,
                        estimatedDuration = task.estimatedDuration,
                        estimatedPoints = task.estimatedPoints,
                    )
                },
            ),
        )
    }

    else -> null // Unknown/future block type — ignore safely
}

internal fun com.awan.app.core.network.dto.goal.GoalDecompositionTranscriptResponse.toTranscript(): com.awan.app.core.model.GoalDecompositionTranscript =
    com.awan.app.core.model.GoalDecompositionTranscript(
        sessionId = sessionId,
        status = status,
        messages = messages.map { msg ->
            com.awan.app.core.model.DecompositionMessage(
                role = msg.role,
                blocks = msg.blocks.mapNotNull { it.toBlock() },
            )
        },
        hasProposal = hasProposal,
        confirmedGoalId = confirmedGoalId,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

