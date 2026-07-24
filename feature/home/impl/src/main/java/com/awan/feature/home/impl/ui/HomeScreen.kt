package com.awan.feature.home.impl.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanHeaderBar
import com.awan.app.core.designsystem.AwanScheduleAlertCard
import com.awan.app.core.designsystem.AwanScheduleTimeline

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
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

            AwanHeaderBar(
                userName = uiState.userName,
                streakCount = uiState.streakCount,
                pointsCount = uiState.pointsCount,
                mascotExpression = uiState.mascotExpression,
                subtitleText = uiState.subtitleText,
                selectedDateText = uiState.selectedDateText,
                onPreviousDayClick = viewModel::previousDay,
                onNextDayClick = viewModel::nextDay,
                onDatePillClick = {
                    if (uiState.isToday) {
                        onNavigateToCalendar()
                    } else {
                        viewModel.selectToday()
                    }
                },
            )

            Spacer(modifier = Modifier.height(16.dp))

            AwanScheduleTimeline(
                zones = uiState.zones,
                sessions = uiState.sessions,
                currentTimeFormatted = uiState.currentTimeFormatted,
                currentTimeMinutes = uiState.currentTimeMinutes,
                isToday = uiState.isToday,
                isPastDate = uiState.isPastDate,
                onToggleZoneCollapse = viewModel::toggleZoneCollapse,
                onAddSessionToZone = viewModel::addSessionToZone,
                onSessionStatusToggle = viewModel::toggleSessionStatus,
                onSessionMoved = viewModel::moveSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }

        if (uiState.hasConflict) {
            Dialog(
                onDismissRequest = viewModel::dismissConflict,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanScheduleAlertCard(
                        message = uiState.conflictMessage,
                        onFixItClick = viewModel::fixConflict,
                        onLaterClick = viewModel::dismissConflict,
                    )
                }
            }
        }
    }
}
