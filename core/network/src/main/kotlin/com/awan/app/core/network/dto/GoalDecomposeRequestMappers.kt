package com.awan.app.core.network.dto

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

fun GoalDecomposeRequest.toJsonObject(): JsonObject = buildJsonObject {
    // include sessionId explicitly even when null (tests expect "sessionId":null)
    put("sessionId", sessionId?.let { JsonPrimitive(it) } ?: JsonNull)
    put("message", JsonPrimitive(message))
}
