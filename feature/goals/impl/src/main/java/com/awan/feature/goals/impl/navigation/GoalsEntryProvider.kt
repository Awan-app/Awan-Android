package com.awan.feature.goals.impl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.app.core.designsystem.AwanSegmentedControl
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.goals.api.GoalDetailsRoute
import com.awan.feature.goals.impl.ui.GoalsScreen
import com.awan.feature.goals.impl.presentation.GoalsViewModel
import com.awan.feature.goals.impl.presentation.GoalDetailsViewModel
import com.awan.feature.goals.impl.ui.GoalDetailsScreen
import com.awan.feature.goals.impl.ui.InboxScreen
import com.awan.feature.goals.impl.presentation.InboxViewModel
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.goals.impl.presentation.GoalsEvent
import com.awan.feature.goals.api.InboxRoute

fun EntryProviderScope<Route>.goalsEntry(
    onNavigateToGoalDetails: (String) -> Unit = {},
    onNavigateToInbox: () -> Unit = {},
    onBack: () -> Unit = {},
) {
    entry<GoalsRoute> {
        GoalsRouteScreen(
            onNavigateToGoalDetails = onNavigateToGoalDetails,
            onNavigateToInbox = onNavigateToInbox
        )
    }

    entry<InboxRoute> {
        InboxRouteScreen(onBack = onBack)
    }

    entry<GoalDetailsRoute> { route ->
        GoalDetailsRouteScreen(
            route = route,
            onBack = onBack
        )
    }
}

@Composable
fun InboxRouteScreen(
    onBack: () -> Unit,
    viewModel: InboxViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    InboxScreen(
        state = state,
        events = viewModel.events,
        onAction = viewModel::onAction,
        onNavigateBack = onBack
    )
}

@Composable
fun GoalDetailsRouteScreen(
    route: GoalDetailsRoute,
    onBack: () -> Unit,
    viewModel: GoalDetailsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(route.id) {
        viewModel.loadGoal(route.id)
    }

    GoalDetailsScreen(
        state = state,
        events = viewModel.events,
        onAction = viewModel::onAction,
        onNavigateBack = onBack
    )
}

@Composable
fun GoalsRouteScreen(
    onNavigateToGoalDetails: (String) -> Unit,
    onNavigateToInbox: () -> Unit,
    goalsViewModel: GoalsViewModel = hiltViewModel(),
) {
    val goalsState by goalsViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(goalsViewModel.events) { event ->
        when (event) {
            is GoalsEvent.NavigateToGoalDetails ->
                onNavigateToGoalDetails(event.goalId)
            GoalsEvent.NavigateToAddGoal -> {
                // TODO: Implement navigation to add goal
            }
            GoalsEvent.NavigateToInbox -> {
                onNavigateToInbox()
            }
            GoalsEvent.OpenMenu -> {
                // TODO: Implement menu opening
            }
        }
    }

    GoalsScreen(
        state = goalsState,
        onAction = goalsViewModel::onAction,
        modifier = Modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
    )
}
