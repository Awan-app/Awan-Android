package com.awan.feature.profile.impl.presentation

import com.awan.app.core.common.text.UiText

sealed interface McpSettingsEvent {
    data class Error(val message: UiText) : McpSettingsEvent
}
