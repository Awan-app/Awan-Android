package com.awan.app.core.network.dto.task

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** [description] is free-text context for the model, not a field it has to echo back. */
@Serializable
data class CreateTaskWithAiRequest(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String? = null,
)