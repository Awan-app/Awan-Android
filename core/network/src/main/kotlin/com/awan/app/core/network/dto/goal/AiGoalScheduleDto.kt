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
    @SerialName("suggestions") val suggestions: List<GoalScheduleSuggestionDto> = emptyList(),
    @SerialName("unscheduledTasks") val unscheduledTasks: List<UnscheduledTaskDto> = emptyList(),
)

@Serializable
data class ProposedGoalSessionDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("taskTitle") val taskTitle: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
)

@Serializable
data class GoalScheduleSuggestionDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("taskTitle") val taskTitle: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("suggestionType") val suggestionType: String,
    @SerialName("reason") val reason: String,
    @SerialName("overlapInfo") val overlapInfo: ScheduleOverlapInfoDto? = null,
)

@Serializable
data class ScheduleOverlapInfoDto(
    @SerialName("taskTitle") val taskTitle: String,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
    @SerialName("mandatory") val mandatory: Boolean,
    @SerialName("points") val points: Int,
)

@Serializable
data class UnscheduledTaskDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("taskTitle") val taskTitle: String,
    @SerialName("message") val message: String,
)

@Serializable
data class ConfirmedGoalSessionDto(
    @SerialName("taskId") val taskId: String,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
)

@Serializable
data class ConfirmAiScheduleRequest(
    @SerialName("goalId") val goalId: String,
    @SerialName("sessions") val sessions: List<ConfirmedGoalSessionDto>,
)

@Serializable
data class AiConfirmedSessionItemDto(
    @SerialName("id") val id: String? = null,
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("locked") val locked: Boolean = false,
) {
    val effectiveId: String
        get() = id?.takeIf { it.isNotBlank() }
            ?: sessionId?.takeIf { it.isNotBlank() }
            ?: java.util.UUID.randomUUID().toString()
}
