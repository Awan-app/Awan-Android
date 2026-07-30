package com.awan.feature.addtask.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.GoalPreviewRouteRoot

fun EntryProviderScope<Route>.goalPreviewEntry(
    viewModel: AddTaskViewModel,
    onBack: () -> Unit,
) {
    entry<GoalPreviewRoute> {
        GoalPreviewRouteRoot(
            viewModel = viewModel,
            onBack = onBack,
        )
    }
}
