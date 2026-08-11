package com.awan.feature.profile.impl.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.profile.impl.presentation.McpSettingsEvent
import com.awan.feature.profile.impl.presentation.McpSettingsViewModel
import com.awan.feature.profile.impl.ui.McpSettingsScreen

@Composable
fun McpSettingsRouteScreen(
    viewModel: McpSettingsViewModel = hiltViewModel(),
    onNavigateToInfo: () -> Unit,
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is McpSettingsEvent.Error -> {
                Toast.makeText(context, event.message.asString(context), Toast.LENGTH_SHORT).show()
            }
            is McpSettingsEvent.TokenCreated -> {}
            McpSettingsEvent.TokenDeleted -> {}
            is McpSettingsEvent.TokenRegenerated -> {}
        }
    }

    McpSettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onInfoClick = onNavigateToInfo,
        onBackClick = onBack,
    )
}
