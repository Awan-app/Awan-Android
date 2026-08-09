package com.awan.app.core.network.dto.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleGoalRequest(
    @SerialName("goalId") val goalId: String,
)

@Serializable
data class AiGoalScheduleProposalResponse(
    @SerialName("goalId") val goalId: String,
    @SerialName("proposedSessions") val proposedSessions: List<ProposedGoalSessionDto> = emptyList(),
    @SerialName("reason") val reason: String? = null,
    @SerialName("timestamp") val timestamp: String? = null,
)

@Serializable
data class ProposedGoalSessionDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
)

@Serializable
data class ConfirmAiScheduleRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("sessions") val sessions: List<ProposedGoalSessionDto>,
)
