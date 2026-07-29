package com.awan.feature.calendar.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.calendar.impl.presentation.CalendarRouteScreen
import java.time.LocalDate

fun EntryProviderScope<Route>.calendarEntry(
    onDateSelected: (LocalDate) -> Unit,
    onBack: () -> Unit,
) {
    entry<CalendarRoute> {
        CalendarRouteScreen(onDateSelected = onDateSelected, onBack = onBack)
    }
}
