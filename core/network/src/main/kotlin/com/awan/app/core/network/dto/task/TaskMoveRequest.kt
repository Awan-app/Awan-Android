package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskMoveRequest(
    @SerialName("goalId") val goalId: String,
)
