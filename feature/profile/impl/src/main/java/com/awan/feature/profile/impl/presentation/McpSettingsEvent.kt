package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.mcp.model.CreatedMcpToken

sealed interface McpSettingsEvent {
    data class TokenCreated(val token: CreatedMcpToken) : McpSettingsEvent
    data object TokenDeleted : McpSettingsEvent
    data class TokenRegenerated(val token: CreatedMcpToken) : McpSettingsEvent
    data class Error(val message: UiText) : McpSettingsEvent
}
