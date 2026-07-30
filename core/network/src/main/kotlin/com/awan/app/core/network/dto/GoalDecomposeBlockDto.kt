package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A raw content block decoded from the `blocks` array in [GoalDecomposeResponse].
 *
 * All fields beyond [type] are optional so unknown/future block types decode safely
 * without crashing. The mapper in `:core:data` converts the raw DTO into the sealed
 * [com.awan.app.core.model.GoalDecompositionBlock] hierarchy, dropping unknown types.
 */
@Serializable
data class GoalDecomposeBlockDto(
    @SerialName("type") val type: String,
    /** Present for "text" and "question" blocks. */
    @SerialName("text") val text: String? = null,
    /** Present (and may be empty) for "question" blocks. Missing means writing-question. */
    @SerialName("options") val options: List<String>? = null,
    /** Present for "proposal" blocks. */
    @SerialName("proposal") val proposal: GoalProposalDto? = null,
)

/** The nested proposal object inside a `"type":"proposal"` block. */
@Serializable
data class GoalProposalDto(
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("targetDate") val targetDate: String? = null,
    @SerialName("tasks") val tasks: List<ProposedTaskDto> = emptyList(),
)

/** A single task inside [GoalProposalDto]. */
@Serializable
data class ProposedTaskDto(
    @SerialName("title") val title: String,
    @SerialName("estimatedDuration") val estimatedDuration: Int? = null,
    @SerialName("estimatedPoints") val estimatedPoints: Int? = null,
)
