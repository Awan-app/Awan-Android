package com.awan.feature.addtask.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.GoalPreviewRouteRoot

import androidx.hilt.navigation.compose.hiltViewModel

fun EntryProviderScope<Route>.goalPreviewEntry(
    onBack: () -> Unit,
    onNavigateToGoals: () -> Unit,
) {
    entry<GoalPreviewRoute> {
        val viewModel: AddTaskViewModel = hiltViewModel()
        GoalPreviewRouteRoot(
            viewModel = viewModel,
            onBack = onBack,
            onNavigateToGoals = onNavigateToGoals,
        )
    }
}
