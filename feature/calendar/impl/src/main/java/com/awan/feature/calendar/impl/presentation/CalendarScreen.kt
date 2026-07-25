package com.awan.feature.calendar.impl.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.Style
import androidx.compose.foundation.style.styleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Flame
import com.composables.icons.lucide.Lucide
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanSurface
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextStyle
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.calendar.impl.R
import com.awan.feature.calendar.impl.model.CalendarGoal
import com.awan.feature.calendar.impl.model.DayState
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun rememberIsReducedMotion(): Boolean {
    var isReduced by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val scaleFactor = coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f
        isReduced = scaleFactor == 0f
    }
    return isReduced
}

@Composable
fun CalendarRouteScreen(
    onDateSelected: (LocalDate) -> Unit,
    onBack: () -> Unit,
    viewModel: CalendarViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            if (event is CalendarEvent.DateSelected) onDateSelected(event.date)
        }
    }
    CalendarScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAction: (CalendarAction) -> Unit,
    onBack: () -> Unit,
) {
    val isReducedMotion = rememberIsReducedMotion()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AwanButton(
                    onClick = onBack,
                    variant = AwanButtonVariant.Secondary,
                    style = Style {
                        minHeight(0.dp)
                        contentPadding(0.dp)
                    },
                    modifier = Modifier.size(42.dp),
                ) {
                    Icon(
                        imageVector = Lucide.ArrowLeft,
                        contentDescription = stringResource(R.string.calendar_back),
                    )
                }
                Spacer(modifier = Modifier.width(AwanTheme.spacing.sm))
                AwanText(
                    text = stringResource(R.string.calendar_title),
                    style = AwanTheme.styles.displayText,
                    modifier = Modifier.testTag("calendar_title"),
                )
            }
            StreakSummaryCard(streak = state.streak)

            MonthHeader(
                yearMonth = state.currentYearMonth,
                onPrevMonth = { onAction(CalendarAction.PreviousMonth) },
                onNextMonth = { onAction(CalendarAction.NextMonth) },
            )

            WeekdayHeader()

            MonthGridContainer(
                days = state.monthDays,
                isReducedMotion = isReducedMotion,
                onSelectDate = { onAction(CalendarAction.SelectDate(it)) },
            )

            Spacer(modifier = Modifier.height(AwanTheme.spacing.xs))

            AwanText(
                text = stringResource(R.string.calendar_upcoming_deadlines),
                style = AwanTheme.styles.headingText,
            )

            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = AwanTheme.colors.sky)
                }
            } else if (state.errorMessage != null) {
                AwanSurface(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(AwanTheme.spacing.sm),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        AwanText(text = stringResource(state.errorMessage), style = AwanTheme.styles.bodyText)
                        Spacer(modifier = Modifier.height(AwanTheme.spacing.xs))
                        AwanButton(
                            onClick = { onAction(CalendarAction.Refresh) },
                            variant = AwanButtonVariant.Secondary,
                        ) {
                            AwanText(stringResource(R.string.calendar_retry))
                        }
                    }
                }
            } else if (state.upcomingGoals.isEmpty()) {
                AwanSurface(modifier = Modifier.fillMaxWidth()) {
                    AwanText(
                        text = stringResource(R.string.calendar_no_deadlines),
                        style = AwanTheme.styles.bodyText,
                        modifier = Modifier.padding(AwanTheme.spacing.sm),
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                    state.upcomingGoals.forEachIndexed { index, goal ->
                        StaggeredGoalItem(
                            index = index,
                            goal = goal,
                            today = state.today,
                            isReducedMotion = isReducedMotion,
                        )
                    }
                }
            }
        }
}

@Composable
private fun StreakSummaryCard(streak: Int) {
    AwanSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_summary_card")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AwanTheme.spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.zoneSun),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Lucide.Flame, contentDescription = null, tint = AwanTheme.colors.textPrimary)
            }
            Spacer(modifier = Modifier.width(AwanTheme.spacing.md))
            Column {
                AwanText(
                    text = stringResource(R.string.calendar_streak_title, streak),
                    style = AwanTheme.styles.titleText,
                )
                AwanText(
                    text = stringResource(R.string.calendar_streak_subtitle),
                    style = AwanTheme.styles.captionText,
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(
    yearMonth: java.time.YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AwanText(
            text = yearMonth.format(monthFormatter),
            style = AwanTheme.styles.titleText,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
            AwanButton(
                onClick = onPrevMonth,
                variant = AwanButtonVariant.Secondary,
                style = Style {
                    minHeight(0.dp)
                    contentPadding(0.dp)
                },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("prev_month_button"),
            ) {
                Icon(
                    imageVector = Lucide.ChevronLeft,
                    contentDescription = stringResource(R.string.calendar_prev_month),
                )
            }
            AwanButton(
                onClick = onNextMonth,
                variant = AwanButtonVariant.Secondary,
                style = Style {
                    minHeight(0.dp)
                    contentPadding(0.dp)
                },
                modifier = Modifier
                    .size(42.dp)
                    .testTag("next_month_button"),
            ) {
                Icon(
                    imageVector = Lucide.ChevronRight,
                    contentDescription = stringResource(R.string.calendar_next_month),
                )
            }
        }    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        weekdays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(text = day, style = AwanTheme.styles.bodyText)
            }
        }
    }
}


@Composable
private fun MonthGridContainer(
    days: List<DayState>,
    isReducedMotion: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val duration = if (isReducedMotion) 0 else 250
    AnimatedContent(
        targetState = days,
        transitionSpec = {
            fadeIn(animationSpec = tween(durationMillis = duration)) togetherWith
                fadeOut(animationSpec = tween(durationMillis = duration))
        },
        label = "MonthGridTransition",
    ) { currentDays ->
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            val rows = currentDays.chunked(7)
            rows.forEach { rowDays ->
                WeekRow(
                    rowDays = rowDays,
                    isReducedMotion = isReducedMotion,
                    onSelectDate = onSelectDate,
                )
            }
        }
    }
}

@Composable
private fun WeekRow(
    rowDays: List<DayState>,
    isReducedMotion: Boolean,
    onSelectDate: (LocalDate) -> Unit,
) {
    val streakRuns = remember(rowDays) {
        val runs = mutableListOf<IntRange>()
        var start = -1
        rowDays.forEachIndexed { i, day ->
            if (day.isStreakDay) {
                if (start == -1) start = i
            } else {
                if (start != -1) {
                    runs.add(start until i)
                    start = -1
                }
            }
        }
        if (start != -1) runs.add(start until rowDays.size)
        runs
    }

    val streakSun = AwanTheme.colors.zoneSun
    val streakCoral = AwanTheme.colors.zoneCoral
    val streakBrush = Brush.horizontalGradient(listOf(streakSun, streakCoral))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val cellWidth = size.width / 7f
                // Top inset (2.dp + 2.dp = 4.dp) plus radius of 36.dp circle (18.dp) = 22.dp center
                val centerY = 4.dp.toPx() + 18.dp.toPx()
                val lineThickness = 34.dp.toPx()

                streakRuns.forEach { run ->
                    val startX = run.start * cellWidth + cellWidth / 2f
                    val endX = run.endInclusive * cellWidth + cellWidth / 2f
                    drawLine(
                        brush = streakBrush,
                        start = Offset(startX, centerY),
                        end = Offset(endX, centerY),
                        strokeWidth = lineThickness,
                        cap = StrokeCap.Round,
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            rowDays.forEach { dayState ->
                DayCell(
                    dayState = dayState,
                    isReducedMotion = isReducedMotion,
                    onSelectDate = onSelectDate,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun DayCell(
    dayState: DayState,
    isReducedMotion: Boolean,
    onSelectDate: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleTarget = if (!isReducedMotion && isPressed) 0.92f else 1.0f
    val scale by animateFloatAsState(
        targetValue = scaleTarget,
        animationSpec = tween(durationMillis = 120, easing = LinearOutSlowInEasing),
        label = "DayCellPressScale",
    )

    val textColor = when {
        dayState.isToday -> AwanTheme.colors.onSky
        dayState.isStreakDay -> Color.White
        !dayState.isCurrentMonth -> AwanTheme.colors.meta
        else -> AwanTheme.colors.textPrimary
    }

    val streakDayString = stringResource(R.string.calendar_streak_day)
    val hasDeadlineString = stringResource(R.string.calendar_has_deadline)
    val dateDescription = remember(dayState, streakDayString, hasDeadlineString) {
        buildString {
            append(dayState.date.dayOfMonth)
            if (dayState.isStreakDay) {
                append(", ")
                append(streakDayString)
            }
            if (dayState.hasDeadline) {
                append(", ")
                append(hasDeadlineString)
            }
        }
    }

    Column(
        modifier = modifier
            .padding(2.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (dayState.isCurrentMonth) 1f else 0.45f
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = { onSelectDate(dayState.date) }
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bodyTextStyle = AwanTheme.typography.body
        val selectionModifier = if (dayState.isSelected && !dayState.isToday) {
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AwanTheme.colors.surface)
                .drawBehind {
                    drawCircle(color = Color(0xFF38BDF8), radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
                }
        } else {
            Modifier.size(36.dp)
        }

        Box(
            modifier = selectionModifier,
            contentAlignment = Alignment.Center,
        ) {
            if (dayState.isToday && dayState.isStreakDay) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.sky),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = dayState.date.dayOfMonth.toString(),
                        style = AwanTextStyle(bodyTextStyle, AwanTheme.colors.onSky),
                        modifier = Modifier.semantics { contentDescription = dateDescription },
                    )
                }
            } else if (dayState.isToday) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.sky),
                    contentAlignment = Alignment.Center,
                ) {
                    AwanText(
                        text = dayState.date.dayOfMonth.toString(),
                        style = AwanTextStyle(bodyTextStyle, AwanTheme.colors.onSky),
                        modifier = Modifier.semantics { contentDescription = dateDescription },
                    )
                }
            } else {
                AwanText(
                    text = dayState.date.dayOfMonth.toString(),
                    style = AwanTextStyle(bodyTextStyle, textColor),
                    modifier = Modifier.semantics { contentDescription = dateDescription },
                )
            }
        }
        if (dayState.hasDeadline) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.zoneCoral),
            )
        } else {
            Spacer(modifier = Modifier.height(7.dp))
        }
    }
}

@Composable
private fun StaggeredGoalItem(
    index: Int,
    goal: CalendarGoal,
    today: LocalDate,
    isReducedMotion: Boolean,
) {
    var visible by remember { mutableStateOf(isReducedMotion) }
    LaunchedEffect(key1 = goal.id, key2 = isReducedMotion) {
        if (!isReducedMotion) {
            val delayMillis = (index * 40L).coerceAtMost(200L)
            delay(delayMillis.milliseconds)
            visible = true
        } else {
            visible = true
        }
    }

    if (isReducedMotion) {
        GoalItemSurface(goal = goal, today = today)
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(150)) +
                slideInVertically(
                    initialOffsetY = { 20 },
                    animationSpec = tween(150)
                ),
        ) {
            GoalItemSurface(goal = goal, today = today)
        }
    }
}

@Composable
private fun GoalItemSurface(
    goal: CalendarGoal,
    today: LocalDate,
) {
    AwanSurface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_item_${goal.id}"),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AwanTheme.spacing.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = goal.title,
                    style = AwanTheme.styles.headingText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                val formatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }
                AwanText(
                    text = stringResource(R.string.calendar_target_date, goal.targetDate.format(formatter)),
                    style = AwanTheme.styles.captionText,
                )
            }
            val daysUntil = ChronoUnit.DAYS.between(today, goal.targetDate)
            val badgeText = when {
                daysUntil == 0L -> stringResource(R.string.calendar_badge_today)
                daysUntil == 1L -> stringResource(R.string.calendar_badge_tomorrow)
                daysUntil > 1L -> stringResource(R.string.calendar_badge_in_days, daysUntil)
                else -> stringResource(R.string.calendar_badge_past)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(AwanTheme.colors.disabledSurface)
                    .padding(horizontal = AwanTheme.spacing.xs, vertical = 4.dp),
            ) {
                AwanText(
                    text = badgeText,
                    style = AwanTheme.styles.captionText,
                )
            }
        }
    }
}

