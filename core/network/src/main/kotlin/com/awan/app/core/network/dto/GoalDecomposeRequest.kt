package com.awan.app.core.network.dto

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST v1/ai/goal-decompose`.
 *
 * The backend requires `"sessionId": null` on the initial decomposition message.
 * [@EncodeDefault] ensures null [sessionId] is always serialized explicitly as `null`
 * rather than being omitted, satisfying the backend contract natively.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class GoalDecomposeRequest(
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("sessionId") val sessionId: String? = null,
    @SerialName("message") val message: String,
)
