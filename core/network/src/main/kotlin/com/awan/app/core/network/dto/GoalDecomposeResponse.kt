package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body for `POST v1/ai/goal-decompose`.
 *
 * [timestamp] is present in the wire format but is transport-only metadata;
 * it is not forwarded to the domain layer.
 */
@Serializable
data class GoalDecomposeResponse(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("blocks") val blocks: List<GoalDecomposeBlockDto> = emptyList(),
    @SerialName("hasProposal") val hasProposal: Boolean = false,
    /** Transport-only. Not mapped to domain. */
    @SerialName("timestamp") val timestamp: String? = null,
)
