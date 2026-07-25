package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskInfoResponse(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
    @SerialName("estimatedDuration") val estimatedDuration: Int? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("mandatory") val mandatory: Boolean? = false,
    @SerialName("estimatedPoints") val estimatedPoints: Int? = 0,
    @SerialName("allowTaskSplitting") val allowTaskSplitting: Boolean? = false,
    @SerialName("goalId") val goalId: String? = null,
    @SerialName("dependsOnTaskIds") val dependsOnTaskIds: List<String>? = emptyList(),
    @SerialName("category") val category: CategoryDto? = null,
)
