package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `persist=false` response from `POST /v1/ai/task-create`. Every field is optional: the task was
 * never saved, so nothing here is guaranteed the way a real `TaskInfoResponse.id` is. A `sessions`
 * array, if the backend sends one, is intentionally not declared here — a preview's proposed timing
 * is never used (scheduling is always a separate, explicit request), and `Json.ignoreUnknownKeys`
 * drops it rather than requiring session fields a draft doesn't have.
 */
@Serializable
data class AiTaskPreviewResponse(
    @SerialName("task") val task: AiTaskPreviewTaskResponse? = null,
)

/** Both [categoryId] and [category] are declared since the echoed shape isn't pinned down. */
@Serializable
data class AiTaskPreviewTaskResponse(
    @SerialName("title") val title: String? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("estimatedDuration") val estimatedDuration: Int? = null,
    @SerialName("mandatory") val mandatory: Boolean? = null,
    @SerialName("estimatedPoints") val estimatedPoints: Int? = null,
    @SerialName("allowTaskSplitting") val allowTaskSplitting: Boolean? = null,
    @SerialName("categoryId") val categoryId: String? = null,
    @SerialName("category") val category: CategoryDto? = null,
)
