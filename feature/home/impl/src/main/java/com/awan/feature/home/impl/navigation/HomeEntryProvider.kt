package com.awan.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.ui.HomeScreen
import java.time.LocalDate

fun EntryProviderScope<Route>.homeEntry(
    onLogout: () -> Unit,
    onNavigateToCalendar: () -> Unit = {},
    onRegisterSelectDate: ((LocalDate) -> Unit) -> Unit = {},
    onNavigateToAddTask: (zoneId: String?, date: LocalDate?) -> Unit = { _, _ -> },
    // Home hands its opener up, the same way it does for date selection. The alternative — passing
    // the pending session down — makes every caller depend on a value that changes after this entry
    // was first composed.
    onRegisterOpenSession: ((String) -> Unit) -> Unit = {},
    onNavigateToTaskDetails: (String) -> Unit = {},
) {
    entry<HomeRoute> {
        HomeScreen(
            onLogout = onLogout,
            onNavigateToCalendar = onNavigateToCalendar,
            onRegisterSelectDate = onRegisterSelectDate,
            onNavigateToAddTask = onNavigateToAddTask,
            onRegisterOpenSession = onRegisterOpenSession,
            onNavigateToTaskDetails = onNavigateToTaskDetails,
        )
    }
}
