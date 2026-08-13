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
    // Held by the caller rather than carried on the route: HomeRoute() is a top-level key, and a
    // HomeRoute with arguments no longer matches it, so the navigator would stack a second Home on
    // whatever tab was open instead of switching to this one.
    deepLinkSessionId: String? = null,
    deepLinkDate: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    entry<HomeRoute> {
        HomeScreen(
            onLogout = onLogout,
            onNavigateToCalendar = onNavigateToCalendar,
            onRegisterSelectDate = onRegisterSelectDate,
            onNavigateToAddTask = onNavigateToAddTask,
            deepLinkSessionId = deepLinkSessionId,
            deepLinkDate = deepLinkDate,
            onDeepLinkHandled = onDeepLinkHandled,
        )
    }
}
