package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskScheduleResponse(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("scheduledSessions") val scheduledSessions: List<ScheduledSessionResponse>? = emptyList(),
    @SerialName("unscheduledTasks") val unscheduledTasks: List<UnscheduledTaskResponse>? = emptyList(),
)

@Serializable
data class ScheduledSessionResponse(
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String? = null,
    @SerialName("end") val end: String? = null,
)

@Serializable
data class UnscheduledTaskResponse(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("taskTitle") val taskTitle: String? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("message") val message: String? = null,
)
