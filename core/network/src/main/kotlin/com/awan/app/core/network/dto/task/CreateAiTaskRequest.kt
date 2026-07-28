package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CreateAiTaskRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
)
