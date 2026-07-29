package com.awan.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class IsNewResponse(
    val isNew: Boolean,
)
