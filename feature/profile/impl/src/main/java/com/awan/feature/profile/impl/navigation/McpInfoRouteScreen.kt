package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.profile.impl.presentation.McpSettingsViewModel
import com.awan.feature.profile.impl.ui.McpInfoScreen

@Composable
fun McpInfoRouteScreen(
    viewModel: McpSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    McpInfoScreen(
        mcpUrl = uiState.connectionDetails?.mcpUrl.orEmpty(),
        onBackClick = onBack
    )
}
