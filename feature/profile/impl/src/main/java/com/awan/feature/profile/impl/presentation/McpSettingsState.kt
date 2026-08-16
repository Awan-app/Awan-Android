package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.domain.mcp.model.McpConnectionDetails

data class McpSettingsState(
    val connectionDetails: McpConnectionDetails? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null,
)
