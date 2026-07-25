package com.awan.feature.goals.impl.navigation

import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.goals.impl.presentation.GoalsScreen
import com.awan.feature.goals.impl.presentation.GoalsViewModel

fun EntryProviderScope<Route>.goalsEntry() {
    entry<GoalsRoute> {
        val viewModel: GoalsViewModel = viewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        GoalsScreen(state = state, onAction = viewModel::onAction)
    }
}
