package com.awan.app.core.network.dto.task

import com.awan.app.core.network.dto.gamification.RewardDto
import com.awan.app.core.network.dto.session.SessionDto
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskCompletionResponse(
    @SerialName("task") val task: TaskInfoResponse,
    @SerialName("completedSessions") val completedSessions: List<SessionDto> = emptyList(),
    @SerialName("reward") val reward: RewardDto? = null
)
