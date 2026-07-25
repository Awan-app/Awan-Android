package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleTaskRequest(
    @SerialName("taskId") val taskId: String,
    @SerialName("horizonDays") val horizonDays: Int? = null,
)

/**
 * A 200 here does not mean the task was placed. When the engine can't fit it inside the horizon the
 * call still succeeds and reports why in [unscheduledTasks], so both lists have to be read.
 */
@Serializable
data class TaskScheduleResponse(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("scheduledSessions") val scheduledSessions: List<ScheduledSessionDto>? = emptyList(),
    @SerialName("unscheduledTasks") val unscheduledTasks: List<UnscheduledTaskDto>? = emptyList(),
)

@Serializable
data class ScheduledSessionDto(
    @SerialName("sessionId") val sessionId: String,
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("zoneId") val zoneId: String? = null,
    @SerialName("start") val start: String,
    @SerialName("end") val end: String,
)

@Serializable
data class UnscheduledTaskDto(
    @SerialName("taskId") val taskId: String? = null,
    @SerialName("taskTitle") val taskTitle: String? = null,
    @SerialName("reason") val reason: String? = null,
    @SerialName("message") val message: String? = null,
)
