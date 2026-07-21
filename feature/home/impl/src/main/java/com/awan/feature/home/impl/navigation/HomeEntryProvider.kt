package com.awan.feature.home.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.ui.HomeScreen

fun EntryProviderScope<Route>.homeEntry(
    onLogout: () -> Unit,
) {
    entry<HomeRoute> {
        HomeScreen(onLogout = onLogout)
    }
}
