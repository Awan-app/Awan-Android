package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.profile.impl.presentation.ProfileEvent
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.ProfileScreen
import kotlinx.coroutines.flow.collectLatest

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onDailyZonesClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ProfileEvent.LogoutSuccess -> onLogout()
                ProfileEvent.UpdateSuccess -> Unit
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        events = viewModel.events,
        onAction = viewModel::onAction,
        onDailyZonesClick = onDailyZonesClick,
        onSettingsClick = { },
    )
}
