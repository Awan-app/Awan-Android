package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TaskUpdateRequest(
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("mandatory") val mandatory: Boolean? = null,
    @SerialName("estimatedDuration") val estimatedDuration: Int? = null,
    @SerialName("estimatedPoints") val estimatedPoints: Int? = null,
    @SerialName("allowTaskSplitting") val allowTaskSplitting: Boolean? = null,
    @SerialName("categoryId") val categoryId: String? = null,
)
