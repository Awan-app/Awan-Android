package com.awan.feature.home.impl.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanHeaderBar
import com.awan.app.core.designsystem.AwanScheduleAlertCard
import com.awan.app.core.designsystem.AwanScheduleTimeline

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF4F7FC)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 18.dp),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Sticky Header Bar (Stays pinned at top on all timeline scrolls)
            AwanHeaderBar(
                userName = uiState.userName,
                streakCount = uiState.streakCount,
                subtitleText = uiState.subtitleText,
                selectedDateText = uiState.selectedDateText,
                onPreviousDayClick = viewModel::previousDay,
                onNextDayClick = viewModel::nextDay,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Alert / Conflict Banner (if active)
            AnimatedVisibility(
                visible = uiState.hasConflict,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            ) {
                Column {
                    AwanScheduleAlertCard(
                        message = uiState.conflictMessage,
                        onFixItClick = viewModel::fixConflict,
                        onLaterClick = viewModel::dismissConflict,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // 3. Vertically Scrollable Timeline (Takes remaining height & scrolls timeline internally)
            AwanScheduleTimeline(
                zones = uiState.zones,
                tasks = uiState.tasks,
                currentTimeFormatted = uiState.currentTimeFormatted,
                currentTimeMinutes = uiState.currentTimeMinutes,
                onToggleZoneCollapse = viewModel::toggleZoneCollapse,
                onTaskStatusToggle = viewModel::toggleTaskStatus,
                onTaskMoved = viewModel::moveTask,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
