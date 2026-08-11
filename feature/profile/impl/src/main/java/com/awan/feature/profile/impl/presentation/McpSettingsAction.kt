package com.awan.feature.profile.impl.presentation

sealed interface McpSettingsAction {
    data class CreateToken(val name: String) : McpSettingsAction
    data class DeleteToken(val id: String) : McpSettingsAction
    data class RegenerateToken(val id: String) : McpSettingsAction
    data object DismissCreatedModal : McpSettingsAction
    data object DismissError : McpSettingsAction
    data object Refresh : McpSettingsAction
}
