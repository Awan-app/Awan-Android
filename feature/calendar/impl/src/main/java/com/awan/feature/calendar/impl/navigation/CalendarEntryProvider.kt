package com.awan.feature.calendar.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.calendar.impl.ui.CalendarScreen

fun EntryProviderScope<Route>.calendarEntry() {
    entry<CalendarRoute> {
        CalendarScreen()
    }
}
