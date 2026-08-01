package com.awan.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.ui.HomeScreen
import java.time.LocalDate

fun EntryProviderScope<Route>.homeEntry(
    onLogout: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
) {
    entry<HomeRoute> { route ->
        HomeScreen(
            initialDate = route.date?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
            onLogout = onLogout,
            onNavigateToCalendar = onNavigateToCalendar,
        )
    }
}
