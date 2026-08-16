package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.awan.feature.profile.impl.presentation.McpSettingsViewModel

@Composable
fun McpInfoRouteScreen(
    viewModel: McpSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    McpSettingsRouteScreen(
        viewModel = viewModel,
        onBack = onBack,
    )
}
