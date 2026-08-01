package com.awan.feature.home.impl.ui

import androidx.compose.animation.Crossfade
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanHeaderBar
import com.awan.app.core.designsystem.AwanScheduleAlertCard
import com.awan.app.core.designsystem.AwanScheduleTimeline
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R
import java.time.LocalDate

private sealed interface TimelineContentState {
    data object Loading : TimelineContentState
    data class Error(val message: UiText) : TimelineContentState
    data object Ready : TimelineContentState
}

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onRegisterSelectDate: ((LocalDate) -> Unit) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        onRegisterSelectDate(viewModel::selectDate)
    }

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
                greetingPrefix = uiState.greetingPrefix.asString(),
                streakCount = uiState.streakCount,
                pointsCount = uiState.pointsCount,
                mascotExpression = uiState.mascotExpression,
                subtitleText = uiState.subtitleText.asString(),
                selectedDateText = uiState.selectedDateText.asString(),
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
                                    color = AwanTheme.colors.sky,
                                    strokeWidth = 3.dp,
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                AwanText(
                                    text = stringResource(R.string.loading_your_schedule),
                                    style = AwanTheme.typography.body.copy(
                                        fontSize = 14.sp,
                                        color = AwanTheme.colors.textSecondary,
                                    ),
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
                                AwanText(text = "☁️", style = AwanTheme.typography.display.copy(fontSize = 48.sp))
                                Spacer(modifier = Modifier.height(16.dp))
                                AwanText(
                                    text = state.message.asString(),
                                    style = AwanTheme.typography.body.copy(
                                        fontSize = 15.sp,
                                        color = AwanTheme.colors.textPrimary,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        lineHeight = 22.sp,
                                    ),
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                AwanButton(
                                    onClick = viewModel::retryLoad,
                                    variant = AwanButtonVariant.Primary,
                                ) {
                                    AwanText(
                                        text = stringResource(R.string.retry),
                                        style = AwanTheme.typography.button,
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
