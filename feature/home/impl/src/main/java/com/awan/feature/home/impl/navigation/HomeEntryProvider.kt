package com.awan.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.ui.HomeScreen

fun EntryProviderScope<Route>.homeEntry(
    onLogout: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    refreshTrigger: Int = 0,
) {
    entry<HomeRoute> {
        HomeScreen(
            onLogout = onLogout,
            onNavigateToCalendar = onNavigateToCalendar,
            refreshTrigger = refreshTrigger,
        )
    }
}
