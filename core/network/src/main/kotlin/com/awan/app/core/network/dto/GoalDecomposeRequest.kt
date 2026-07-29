package com.awan.app.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * Request body for `POST v1/ai/goal-decompose`.
 *
 * The backend requires `"sessionId": null` on the initial decomposition message.
 * [toJsonObject] converts this DTO into a [JsonObject] with explicit [JsonNull] for null [sessionId],
 * preserving the required null on initial calls without altering global Json settings.
 */
@Serializable
data class GoalDecomposeRequest(
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("message") val message: String,
) {
    fun toJsonObject(): JsonObject = buildJsonObject {
        put("sessionId", sessionId?.let { JsonPrimitive(it) } ?: JsonNull)
        put("message", JsonPrimitive(message))
    }
}
