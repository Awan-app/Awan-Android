package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.ProfileScreen

fun EntryProviderScope<Route>.profileEntry() {
    entry<ProfileRoute> {
        ProfileRouteScreen()
    }
}

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    ProfileScreen(
        uiState = uiState,
        onEditClick = { /* TODO */ },
        onDailyZonesClick = { /* TODO */ },
        onPreferenceClick = { /* TODO */ },
        onSettingsClick = { /* TODO */ }
    )
}
