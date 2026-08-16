package com.awan.app.core.network.dto.goal

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateGoalRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("targetDate") val targetDate: String? = null,
    @SerialName("tasks") val tasks: List<CreateGoalTaskDto> = emptyList(),
)

@Serializable
data class CreateGoalTaskDto(
    @SerialName("tempId") val tempId: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("estimatedDuration") val estimatedDuration: Int = 30,
    @SerialName("mandatory") val mandatory: Boolean = false,
    @SerialName("estimatedPoints") val estimatedPoints: Int = 0,
    @SerialName("allowTaskSplitting") val allowTaskSplitting: Boolean = false,
    @SerialName("dependsOnTempIds") val dependsOnTempIds: List<String> = emptyList(),
    @SerialName("categoryId") val categoryId: String? = null,
)
