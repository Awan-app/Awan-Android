package com.awan.feature.goals.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.goals.impl.ui.GoalsScreen
import com.awan.feature.goals.impl.presentation.GoalsViewModel

fun EntryProviderScope<Route>.goalsEntry() {
    entry<GoalsRoute> {
        GoalsRouteScreen()
    }
}

@Composable
fun GoalsRouteScreen(
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    GoalsScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}
