package com.awan.feature.goals.impl.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.core.navigation.Route
import com.awan.feature.goals.api.GoalsRoute
import com.awan.feature.goals.api.GoalDetailsRoute
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Brush
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.ui.GoalsScreen
import com.awan.feature.goals.impl.presentation.GoalsViewModel
import com.awan.feature.goals.impl.presentation.InboxScreen
import com.awan.feature.goals.impl.presentation.InboxViewModel
import com.awan.feature.goals.impl.presentation.GoalDetailsViewModel
import com.awan.feature.goals.impl.ui.GoalDetailsScreen
import com.awan.feature.goals.impl.presentation.GoalDetailsAction
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.feature.goals.impl.presentation.GoalsEvent
import com.composables.icons.lucide.Inbox
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target

fun EntryProviderScope<Route>.goalsEntry(
    onNavigateToGoalDetails: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    entry<GoalsRoute> {
        GoalsRouteScreen(
            onNavigateToGoalDetails = onNavigateToGoalDetails,
        )
    }

    entry<GoalDetailsRoute> { route ->
        val viewModel: GoalDetailsViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()

        androidx.compose.runtime.LaunchedEffect(route.id) {
            viewModel.loadGoal(route.id)
        }

        GoalDetailsScreen(
            state = state,
            onAction = { action ->
                when (action) {
                    GoalDetailsAction.Back -> onBack()
                    else -> viewModel.onAction(action)
                }
            }
        )
    }
}

enum class GoalsTopLevelTab {
    Goals, Inbox
}

@Composable
fun GoalsRouteScreen(
    onNavigateToGoalDetails: (String) -> Unit,
    goalsViewModel: GoalsViewModel = hiltViewModel(),
    inboxViewModel: InboxViewModel = hiltViewModel(),
) {
    val goalsState by goalsViewModel.state.collectAsStateWithLifecycle()
    val inboxState by inboxViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(goalsViewModel.events) { event ->
        when (event) {
            is GoalsEvent.NavigateToGoalDetails ->
                onNavigateToGoalDetails(event.goalId)
        }
    }
    
    var selectedTab by rememberSaveable { mutableStateOf(GoalsTopLevelTab.Goals) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        AwanTheme.colors.backgroundStart,
                        AwanTheme.colors.background,
                        AwanTheme.colors.background,
                    ),
                ),
            )
            .statusBarsPadding()
    ) {
        // Shared Top Level Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            val titleText = when (selectedTab) {
                GoalsTopLevelTab.Goals -> stringResource(R.string.goals_title_count)
                GoalsTopLevelTab.Inbox -> stringResource(R.string.inbox_title)
            }
            AwanText(
                text = titleText,
                style = AwanTheme.typography.title.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.ink,
                ),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Level Segmented Control
        val reduced = com.awan.app.core.designsystem.reducedMotion()
        val slide by androidx.compose.animation.core.animateFloatAsState(
            targetValue = if (selectedTab == GoalsTopLevelTab.Goals) 0f else 1f,
            animationSpec = if (reduced) androidx.compose.animation.core.snap() else AwanTheme.motion.settle.spec(),
            label = "tabSlide",
        )

        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(46.dp)
                .clip(AwanTheme.shapes.pill)
                .background(AwanTheme.colors.disabledSurface)
                .padding(4.dp),
        ) {
            val halfWidth = (maxWidth - 8.dp) / 2

            Box(
                modifier = Modifier
                    .offset { androidx.compose.ui.unit.IntOffset(x = (halfWidth * slide).roundToPx(), y = 0) }
                    .width(halfWidth)
                    .fillMaxHeight()
                    .clip(AwanTheme.shapes.pill)
                    .background(AwanTheme.colors.surface),
            )

            Row(Modifier.fillMaxWidth().fillMaxHeight()) {
                GoalsTopLevelTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val text = when (tab) {
                        GoalsTopLevelTab.Goals -> stringResource(R.string.goals_title_count)
                        GoalsTopLevelTab.Inbox -> stringResource(R.string.inbox_title)
                    }
                    val contentColor by androidx.compose.animation.animateColorAsState(
                        targetValue = if (isSelected) AwanTheme.colors.textPrimary else AwanTheme.colors.textSecondary,
                        animationSpec = AwanTheme.motion.settle.spec(),
                        label = "tabContent",
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(AwanTheme.shapes.pill)
                            .selectable(
                                selected = isSelected,
                                onClick = { selectedTab = tab },
                                role = Role.Tab,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        AwanText(
                            text = text,
                            style = AwanTheme.styles.buttonCompactText.copy(color = contentColor)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (selectedTab) {
                GoalsTopLevelTab.Goals -> {
                    GoalsScreen(
                        state = goalsState,
                        onAction = goalsViewModel::onAction,
                    )
                }
                GoalsTopLevelTab.Inbox -> {
                    InboxScreen(
                        state = inboxState,
                        onAction = inboxViewModel::onAction,
                    )
                }
            }
        }
    }
}
