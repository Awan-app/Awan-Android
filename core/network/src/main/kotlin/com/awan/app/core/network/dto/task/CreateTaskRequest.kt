package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * [title] defaults to blank rather than being required: this type doubles as the shape a proposal's
 * `draft.task` decodes into, and a single malformed proposal (missing title) must degrade to one
 * blank-titled card rather than throwing away the whole `TaskProposalResponse`.
 */
@Serializable
data class CreateTaskRequest(
    @SerialName("title") val title: String = "",
    @SerialName("description") val description: String? = null,
    @SerialName("estimatedDuration") val estimatedDuration: Int? = null,
    @SerialName("mandatory") val mandatory: Boolean? = false,
    @SerialName("estimatedPoints") val estimatedPoints: Int? = 0,
    @SerialName("allowTaskSplitting") val allowTaskSplitting: Boolean? = false,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("goalId") val goalId: String? = null,
)
