package com.awan.feature.addtask.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.addtask.ui.GoalPreviewRouteRoot

fun EntryProviderScope<Route>.goalPreviewEntry(
    onBack: () -> Unit,
    onNavigateToGoals: () -> Unit,
) {
    entry<GoalPreviewRoute> {
        GoalPreviewRouteRoot(
            onBack = onBack,
            onNavigateToGoals = onNavigateToGoals,
        )
    }
}
