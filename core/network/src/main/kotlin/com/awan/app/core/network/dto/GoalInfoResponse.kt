package com.awan.app.core.network.dto

import com.awan.app.core.network.dto.task.TaskInfoResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalInfoResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: GoalStatusDto = GoalStatusDto.UNKNOWN,
    @SerialName("targetDate") val targetDate: String? = null,
    @SerialName("createdAt") val createdAt: String? = null,
    @SerialName("inbox") val inbox: Boolean = false,
    @SerialName("tasks") val tasks: List<TaskInfoResponse>? = null,
)
