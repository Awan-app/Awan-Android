package com.awan.feature.profile.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.api.McpInfoRoute
import com.awan.feature.profile.api.McpSettingsRoute
import com.awan.feature.profile.api.ProfileRoute

fun EntryProviderScope<Route>.profileEntry(
    onNavigateToDailyZones: () -> Unit,
    onNavigateToEditRoutine: (String?, String?) -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToMcpSettings: () -> Unit,
    onNavigateToMcpInfo: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onDailyZonesClick = onNavigateToDailyZones,
            onInventoryClick = onNavigateToInventory,
            onNavigateToMcpSettings = onNavigateToMcpSettings,
            onLogout = onLogout
        )
    }

    entry<DailyZonesRoute> {
        DailyZonesRouteScreen(
            onRoutineClick = { onNavigateToEditRoutine(it, null) },
            onCreateRoutineClick = { templateId, date -> onNavigateToEditRoutine(templateId, date) },
            onBack = onBack
        )
    }

    entry<EditRoutineRoute> { route ->
        EditRoutineRouteScreen(
            templateId = route.templateId,
            date = route.date,
            onBack = onBack
        )
    }

    entry<McpSettingsRoute> {
        McpSettingsRouteScreen(
            onNavigateToInfo = onNavigateToMcpInfo,
            onBack = onBack
        )
    }

    entry<McpInfoRoute> {
        McpInfoRouteScreen(
            onBack = onBack
        )
    }
}
