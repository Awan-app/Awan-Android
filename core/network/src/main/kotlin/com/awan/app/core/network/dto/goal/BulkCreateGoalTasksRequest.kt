package com.awan.app.core.network.dto.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BulkCreateGoalTasksRequest(
    @SerialName("tasks") val tasks: List<BulkCreateGoalTaskDto>,
)

@Serializable
data class BulkCreateGoalTaskDto(
    @SerialName("tempId") val tempId: String,
    @SerialName("title") val title: String,
    @SerialName("estimatedDuration") val estimatedDuration: Int,
    @SerialName("mandatory") val mandatory: Boolean,
    @SerialName("estimatedPoints") val estimatedPoints: Int,
    @SerialName("dependsOnRefs") val dependsOnRefs: List<String>,
)
