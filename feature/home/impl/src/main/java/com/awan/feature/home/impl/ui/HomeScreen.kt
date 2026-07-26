package com.awan.feature.home.impl.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanHeaderBar
import com.awan.app.core.designsystem.AwanScheduleAlertCard
import com.awan.app.core.designsystem.AwanScheduleTimeline
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R

private sealed interface TimelineContentState {
    data object Loading : TimelineContentState
    data class Error(val message: String) : TimelineContentState
    data object Ready : TimelineContentState
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val timelineScrollState = rememberScrollState()
    val isHeaderCollapsed by remember { derivedStateOf { timelineScrollState.value > 80 } }

    val contentState: TimelineContentState = when {
        uiState.isLoading                 -> TimelineContentState.Loading
        uiState.errorMessage != null      -> TimelineContentState.Error(uiState.errorMessage!!)
        else                              -> TimelineContentState.Ready
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            AwanHeaderBar(
                userName = uiState.userName,
                greetingPrefix = uiState.greetingPrefix,
                streakCount = uiState.streakCount,
                pointsCount = uiState.pointsCount,
                mascotExpression = uiState.mascotExpression,
                subtitleText = uiState.subtitleText,
                selectedDateText = uiState.selectedDateText,
                isCollapsed = isHeaderCollapsed,
                totalSessionsCount = uiState.sessions.size,
                completedSessionsCount = uiState.completedSessionsCount,
                completedHours = uiState.completedHours,
                totalHours = uiState.totalHours,
                scheduledHoursText = uiState.scheduledHoursText,
                progressSegments = uiState.progressSegments,
                onPreviousDayClick = viewModel::previousDay,
                onNextDayClick = viewModel::nextDay,
                onDatePillClick = {
                    if (uiState.isToday) onNavigateToCalendar() else viewModel.selectToday()
                },
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Crossfade(
                targetState = contentState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                label = "timeline_content",
            ) { state ->
                when (state) {
                    TimelineContentState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(48.dp),
                                    color = Color(0xFF2E8BFF),
                                    strokeWidth = 3.dp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.loading_your_schedule),
                                    fontSize = 14.sp,
                                    color = Color(0xFF64748B),
                                )
                            }
                        }
                    }

                    is TimelineContentState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            ) {
                                Text(text = "☁️", fontSize = 48.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = state.message,
                                    fontSize = 15.sp,
                                    color = Color(0xFF334155),
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp,
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(
                                    onClick = viewModel::retryLoad,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF2E8BFF),
                                    ),
                                ) {
                                    Text(
                                        text = stringResource(R.string.retry),
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    TimelineContentState.Ready -> {
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
                            onReorderSessionsInZone = viewModel::reorderSessionsInZone,
                            scrollState = timelineScrollState,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
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
