package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.api.ProfileRoute

fun EntryProviderScope<Route>.profileEntry(
    onNavigateToDailyZones: () -> Unit,
    onNavigateToEditRoutine: (String?) -> Unit,
    onNavigateToInventory: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onDailyZonesClick = onNavigateToDailyZones,
            onInventoryClick = onNavigateToInventory,
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
}
