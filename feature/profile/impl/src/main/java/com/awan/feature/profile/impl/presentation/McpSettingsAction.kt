package com.awan.feature.profile.impl.presentation

sealed interface McpSettingsAction {
    data object Refresh : McpSettingsAction
    data object DismissError : McpSettingsAction
}
