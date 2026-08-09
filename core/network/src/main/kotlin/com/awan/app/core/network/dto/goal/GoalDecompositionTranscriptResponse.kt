package com.awan.app.core.network.dto.goal

import com.awan.app.core.network.dto.GoalDecomposeBlockDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalDecompositionTranscriptResponse(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("status") val status: String,
    @SerialName("messages") val messages: List<DecompositionMessageDto> = emptyList(),
    @SerialName("hasProposal") val hasProposal: Boolean = false,
    @SerialName("confirmedGoalId") val confirmedGoalId: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("updatedAt") val updatedAt: String? = null,
)

@Serializable
data class DecompositionMessageDto(
    @SerialName("role") val role: String,
    @SerialName("blocks") val blocks: List<GoalDecomposeBlockDto> = emptyList(),
)
