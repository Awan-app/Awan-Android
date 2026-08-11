package com.awan.feature.profile.impl.presentation

import com.awan.app.core.domain.mcp.model.McpToken

sealed interface McpSettingsAction {
    data object ShowAddTokenDialog : McpSettingsAction
    data object HideAddTokenDialog : McpSettingsAction
    data class UpdateNewTokenName(val name: String) : McpSettingsAction
    data class ShowDeleteDialog(val token: McpToken) : McpSettingsAction
    data object HideDeleteDialog : McpSettingsAction
    data class ShowRegenerateDialog(val token: McpToken) : McpSettingsAction
    data object HideRegenerateDialog : McpSettingsAction
    data class CreateToken(val name: String) : McpSettingsAction
    data class DeleteToken(val id: String) : McpSettingsAction
    data class RegenerateToken(val id: String) : McpSettingsAction
    data object DismissCreatedModal : McpSettingsAction
    data object DismissError : McpSettingsAction
    data object Refresh : McpSettingsAction
}
