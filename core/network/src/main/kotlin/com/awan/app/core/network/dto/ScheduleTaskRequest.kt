package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScheduleTaskRequest(
    @SerialName("taskId") val taskId: String,
    @SerialName("horizonDays") val horizonDays: Int? = null,
)
