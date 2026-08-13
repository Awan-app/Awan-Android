package com.awan.feature.calendar.impl.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanSurface
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextStyle
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.awanButtonHaptic
import com.awan.feature.calendar.impl.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.milliseconds



@Composable
private fun CalendarIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
    icon: @Composable () -> Unit,
) {
    val colors = AwanTheme.colors
    val shape = RoundedCornerShape(12.dp)
    val hapticFeedback = LocalHapticFeedback.current
    val haptic = awanButtonHaptic(AwanButtonVariant.Secondary)
    val scope = rememberCoroutineScope()
    val pressAnim = remember { Animatable(0f) }

    val currentTopInset = (2.5.dp * pressAnim.value)
    val currentBottomPadding = (2.5.dp * (1f - pressAnim.value))

    Box(
        modifier = modifier
            .size(42.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = {
                    haptic.let(hapticFeedback::performHapticFeedback)
                    scope.launch {
                        pressAnim.animateTo(1f, animationSpec = tween(40, easing = LinearOutSlowInEasing))
                        pressAnim.animateTo(0f, animationSpec = tween(60, easing = LinearOutSlowInEasing))
                    }
                    onClick()
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = currentTopInset)
                .clip(shape)
                .background(colors.line)
        )
        CompositionLocalProvider(LocalContentColor provides colors.textPrimary) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(top = currentTopInset, bottom = currentBottomPadding)
                    .clip(shape)
                    .background(colors.surface)
                    .border(2.dp, colors.line, shape),
                contentAlignment = Alignment.Center,
                content = { icon() },
            )
        }
    }
}

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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.lg),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md),
    ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CalendarIconButton(
                    onClick = onBack,
                    contentDescription = stringResource(R.string.calendar_back),
                ) {
                    Icon(
                        imageVector = if (isRtl) Lucide.ArrowRight else Lucide.ArrowLeft,
                        contentDescription = null,
                    )
                }
                Spacer(modifier = Modifier.width(AwanTheme.spacing.sm))
                AwanText(
                    text = stringResource(R.string.calendar_title),
                    style = AwanTextStyle(AwanTheme.typography.title.copy(fontSize = 26.sp), AwanTheme.colors.textPrimary),
                    modifier = Modifier.testTag("calendar_title"),
                )
            }
            StreakHeaderCard(state = state.streakHeaderState, streak = state.streak)

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
            Spacer(modifier = Modifier.height(100.dp))
        }
}

private data class CalendarStreakHeaderVisual(
    val faceColor: Color,
    val rimColor: Color,
    val circleColor: Color,
    val titleText: String,
    val subtitleText: String,
)

@Composable
private fun StreakHeaderCard(
    state: CalendarStreakHeaderState,
    streak: Int,
) {
    val colors = AwanTheme.colors
    val variantTag = "streak_header_${state.name.lowercase()}"
    val visual = when (state) {
        CalendarStreakHeaderState.Start -> CalendarStreakHeaderVisual(
            faceColor = colors.sky.copy(alpha = 0.12f),
            rimColor = colors.sky.copy(alpha = 0.35f),
            circleColor = colors.sky.copy(alpha = 0.20f),
            titleText = stringResource(R.string.calendar_streak_start_title),
            subtitleText = stringResource(R.string.calendar_streak_start_subtitle),
        )
        CalendarStreakHeaderState.Restart -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneCoral.copy(alpha = 0.12f),
            rimColor = colors.zoneCoral.copy(alpha = 0.35f),
            circleColor = colors.zoneCoral.copy(alpha = 0.20f),
            titleText = stringResource(R.string.calendar_streak_restart_title),
            subtitleText = stringResource(R.string.calendar_streak_restart_subtitle),
        )
        CalendarStreakHeaderState.Protect -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneTangerine.copy(alpha = 0.14f),
            rimColor = colors.zoneTangerine.copy(alpha = 0.45f),
            circleColor = colors.zoneTangerine.copy(alpha = 0.25f),
            titleText = stringResource(R.string.calendar_streak_protect_title, streak),
            subtitleText = stringResource(R.string.calendar_streak_protect_subtitle),
        )
        CalendarStreakHeaderState.Celebrate -> CalendarStreakHeaderVisual(
            faceColor = colors.zoneSun.copy(alpha = 0.16f),
            rimColor = colors.zoneSun.copy(alpha = 0.50f),
            circleColor = colors.zoneCoral.copy(alpha = 0.30f),
            titleText = stringResource(R.string.calendar_streak_celebrate_title, streak),
            subtitleText = stringResource(R.string.calendar_streak_celebrate_subtitle),
        )
    }

    AwanCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streak_summary_card"),
        background = visual.faceColor,
        customRimColor = visual.rimColor,
        contentPadding = PaddingValues(AwanTheme.spacing.xs),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(variantTag),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            val outerSize = if (state == CalendarStreakHeaderState.Restart) 44.dp else 48.dp
            val innerSize = if (state == CalendarStreakHeaderState.Restart) 32.dp else 36.dp

            Box(
                modifier = Modifier
                    .size(outerSize)
                    .clip(CircleShape)
                    .then(
                        if (state == CalendarStreakHeaderState.Protect) {
                            Modifier
                                .background(visual.circleColor.copy(alpha = 0.15f))
                                .border(2.dp, colors.zoneTangerine.copy(alpha = 0.6f), CircleShape)
                        } else {
                            Modifier.background(visual.circleColor)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .background(visual.circleColor.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    when (state) {
                        CalendarStreakHeaderState.Start -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.sky,
                            modifier = Modifier.testTag("streak_header_start_icon"),
                        )
                        CalendarStreakHeaderState.Restart -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.zoneCoral,
                            modifier = Modifier.testTag("streak_header_restart_icon"),
                        )
                        CalendarStreakHeaderState.Protect -> Icon(
                            painter = painterResource(id = R.drawable.ic_flame_filled),
                            contentDescription = null,
                            tint = colors.zoneTangerine,
                            modifier = Modifier.testTag("streak_header_protect_icon"),
                        )
                        CalendarStreakHeaderState.Celebrate -> Box(
                            modifier = Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(colors.zoneSun.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_flame_filled),
                                contentDescription = null,
                                tint = colors.zoneSun,
                                modifier = Modifier.testTag("streak_header_celebrate_icon"),
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(AwanTheme.spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = visual.titleText,
                    style = AwanTheme.styles.titleText,
                )
                Spacer(modifier = Modifier.height(2.dp))
                AwanText(
                    text = visual.subtitleText,
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
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
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
            CalendarIconButton(
                onClick = onPrevMonth,
                contentDescription = stringResource(R.string.calendar_prev_month),
                testTag = "prev_month_button",
            ) {
                Icon(
                    imageVector = if (isRtl) Lucide.ChevronRight else Lucide.ChevronLeft,
                    contentDescription = null,
                )
            }
            CalendarIconButton(
                onClick = onNextMonth,
                contentDescription = stringResource(R.string.calendar_next_month),
                testTag = "next_month_button",
            ) {
                Icon(
                    imageVector = if (isRtl) Lucide.ChevronLeft else Lucide.ChevronRight,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun WeekdayHeader() {
    val weekdays = listOf(
        R.string.calendar_weekday_sun,
        R.string.calendar_weekday_mon,
        R.string.calendar_weekday_tue,
        R.string.calendar_weekday_wed,
        R.string.calendar_weekday_thu,
        R.string.calendar_weekday_fri,
        R.string.calendar_weekday_sat,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        weekdays.forEach { day ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(text = stringResource(day), style = AwanTheme.styles.bodyText)
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
            .testTag("calendar_day_${dayState.date}")
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val bodyTextStyle = AwanTheme.typography.body
        val selectionOutline = AwanTheme.colors.sky
        val selectionModifier = if (dayState.isSelected && !dayState.isToday) {
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(AwanTheme.colors.surface)
                .drawBehind {
                    drawCircle(color = selectionOutline, radius = size.minDimension / 2f, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))
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
