package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.mcp.model.CreatedMcpToken
import com.awan.app.core.domain.mcp.model.McpConnectionDetails
import com.awan.app.core.domain.mcp.model.McpToken

data class McpSettingsState(
    val connectionDetails: McpConnectionDetails? = null,
    val tokens: List<McpToken> = emptyList(),
    val createdToken: CreatedMcpToken? = null,
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val userMessage: UiText? = null,
    val error: UiText? = null,
)
