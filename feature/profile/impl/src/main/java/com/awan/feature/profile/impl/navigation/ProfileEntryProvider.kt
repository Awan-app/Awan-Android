package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.CategoryManagementRoute
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.impl.presentation.CategoryManagementViewModel
import com.awan.feature.profile.impl.ui.CategoryManagementScreen

fun EntryProviderScope<Route>.profileEntry(
    onNavigateToDailyZones: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToEditRoutine: (String?) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onDailyZonesClick = onNavigateToDailyZones,
            onCategoryManagementClick = onNavigateToCategoryManagement,
            onLogout = onLogout
        )
    }

    entry<DailyZonesRoute> {
        DailyZonesRouteScreen(
            onRoutineClick = { onNavigateToEditRoutine(it) },
            onCreateRoutineClick = { onNavigateToEditRoutine(null) },
            onBack = onBack
        )
    }

    entry<EditRoutineRoute> { route ->
        EditRoutineRouteScreen(
            templateId = route.templateId,
            onBack = onBack
        )
    }

    entry<CategoryManagementRoute> {
        CategoryManagementRouteScreen(onBack = onBack)
    }
}

@Composable
private fun CategoryManagementRouteScreen(
    onBack: () -> Unit,
    viewModel: CategoryManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    CategoryManagementScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack
    )
}
