package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.ProfileScreen

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onDailyZonesClick: () -> Unit,
    onInventoryClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ProfileScreen(
        uiState = uiState,
        events = viewModel.events,
        onAction = viewModel::onAction,
        onDailyZonesClick = onDailyZonesClick,
        onInventoryClick = onInventoryClick,
        onSettingsClick = { },
        onLogout = onLogout,
    )
}
