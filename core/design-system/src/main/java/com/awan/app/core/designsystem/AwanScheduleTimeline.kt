package com.awan.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@Composable
fun AwanScheduleTimeline(
    zones: List<ScheduleZone>,
    sessions: List<ScheduleSession>,
    currentTimeFormatted: String = "",
    currentTimeMinutes: Int = 0,
    isToday: Boolean = true,
    isPastDate: Boolean = false,
    startHour: Int = 0,
    endHour: Int = 24,
    hourHeightDp: Int = 64,
    intervalMinutes: Int = 15,
    onToggleZoneCollapse: (zoneId: String) -> Unit = {},
    onAddSessionToZone: (zoneId: String) -> Unit = {},
    onSessionStatusToggle: (sessionId: String) -> Unit = {},
    onSessionClick: (sessionId: String) -> Unit = {},
    onSessionMoved: (sessionId: String, newStartMinutes: Int) -> Unit = { _, _ -> },
    onReorderSessionsInZone: (zoneId: String, fromIndex: Int, toIndex: Int) -> Unit = { _, _, _ -> },
    scrollState: ScrollState = rememberScrollState(),
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val totalHours = (endHour - startHour).coerceAtLeast(1)
    val coroutineScope = rememberCoroutineScope()

    val contentTopPadding = 16.dp
    val contentBottomPadding = 160.dp
    var viewportHeightPx by remember { mutableFloatStateOf(0f) }

    val minZoom = 0.35f
    val maxZoom = 4.00f
    var zoomLevel by remember { mutableFloatStateOf(1f) }

    val baseHourHeightPx = with(density) { hourHeightDp.dp.toPx() }
    val targetHourHeightPx = (baseHourHeightPx * zoomLevel).coerceIn(
        baseHourHeightPx * minZoom,
        baseHourHeightPx * maxZoom,
    )

    val transformableState = rememberTransformableState { zoomChange, _, _ ->
        val prevPx = baseHourHeightPx * zoomLevel
        val newZoom = (zoomLevel * zoomChange).coerceIn(minZoom, maxZoom)
        val newPx = baseHourHeightPx * newZoom
        zoomLevel = newZoom

        val currentScroll = scrollState.value.toFloat()
        val topPaddingPx = with(density) { contentTopPadding.toPx() }
        val visibleCenterPx = currentScroll + (viewportHeightPx / 2f)
        val contentCenterPx = (visibleCenterPx - topPaddingPx).coerceAtLeast(0f)
        val scaledContentCenterPx = contentCenterPx * (newPx / prevPx)
        val newScroll = (scaledContentCenterPx + topPaddingPx - (viewportHeightPx / 2f)).coerceAtLeast(0f)
        val scrollDelta = newScroll - currentScroll
        coroutineScope.launch { scrollState.scrollBy(scrollDelta) }
    }

    val effectiveHourHeightPx by animateFloatAsState(
        targetValue = targetHourHeightPx,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy,
        ),
        label = "zoomSmooth",
    )

    val effectiveHourHeightDp = with(density) { effectiveHourHeightPx.toDp() }
    val effectiveHourHeightDpValue = effectiveHourHeightDp.value
    val totalTimelineHeight = effectiveHourHeightDp * totalHours

    var hasInitialScrolled by remember { mutableStateOf(false) }
    LaunchedEffect(sessions, currentTimeMinutes, isToday) {
        if (!hasInitialScrolled) {
            val firstSessionStartMins = sessions.minOfOrNull { it.startMinutes }
            val targetMinutes = firstSessionStartMins ?: if (isToday && currentTimeMinutes > 0) currentTimeMinutes else null
            if (targetMinutes != null) {
                hasInitialScrolled = true
                val topPaddingPx = with(density) { contentTopPadding.toPx() }
                val targetScrollPx = with(density) {
                    (topPaddingPx + (targetMinutes / 60.0) * effectiveHourHeightDp.toPx() - 32.dp.toPx()).toInt()
                }
                scrollState.scrollTo(targetScrollPx.coerceAtLeast(0))
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "livingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f, targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ), label = "pulseAlpha",
    )

    val activeZone = remember(zones, currentTimeMinutes) {
        zones.find { currentTimeMinutes in it.startMinutes..it.endMinutes }
    }
    val activePointerColor = activeZone?.category?.color ?: AwanTheme.colors.sky

    val currentPointerY = remember(isToday, currentTimeMinutes, effectiveHourHeightDpValue) {
        if (isToday && currentTimeFormatted.isNotBlank()
            && currentTimeMinutes in (startHour * 60)..(endHour * 60)
        ) {
            val h = currentTimeMinutes / 60
            val frac = (currentTimeMinutes - h * 60).toFloat() / 60f
            contentTopPadding + ((h * effectiveHourHeightDpValue) + (effectiveHourHeightDpValue * frac)).dp
        } else null
    }

    val hourYOffsets = remember(startHour, endHour, effectiveHourHeightDpValue) {
        (startHour..endHour).associateWith { contentTopPadding + (it * effectiveHourHeightDpValue).dp }
    }
    val hourSlotHeights = remember(startHour, endHour, effectiveHourHeightDpValue) {
        (startHour until endHour).associateWith { effectiveHourHeightDp }
    }

    val positionedSessions = remember(sessions) {
        TimelineLayoutEngine.calculateLayout(sessions)
    }

    val axisWidthDp = 56.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                viewportHeightPx = coords.size.height.toFloat()
            },
        contentAlignment = Alignment.TopEnd,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .transformable(state = transformableState, lockRotationOnZoomPan = true)
                .verticalScroll(scrollState),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(contentTopPadding + totalTimelineHeight + contentBottomPadding + 20.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = axisWidthDp),
                    ) {
                        zones.forEach { zone ->
                            val topY = contentTopPadding + ((zone.startMinutes.toDouble() / 60.0) * effectiveHourHeightDpValue).dp
                            val bandHeight = (((zone.endMinutes - zone.startMinutes).toDouble() / 60.0) * effectiveHourHeightDpValue).dp

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .offset(y = topY)
                                    .height(bandHeight)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(zone.category.color.copy(alpha = 0.07f)),
                            )
                        }
                    }

                    zones.forEach { zone ->
                        val topY = contentTopPadding + ((zone.startMinutes.toDouble() / 60.0) * effectiveHourHeightDpValue).dp

                        AwanText(
                            text = zone.category.name.uppercase(),
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = zone.category.color,
                                letterSpacing = 0.8.sp,
                            ),
                            modifier = Modifier
                                .offset(y = topY + 5.dp)
                                .padding(start = axisWidthDp + 10.dp),
                        )
                    }

                    val subIntervalMins = when {
                        effectiveHourHeightDpValue >= 120 -> 5
                        effectiveHourHeightDpValue >= 60  -> 10
                        else                              -> 0
                    }

                    TimelineHourAxis(
                        startHour = startHour,
                        endHour = endHour,
                        hourYOffsets = hourYOffsets,
                        hourSlotHeights = hourSlotHeights,
                        zones = zones,
                        sessions = sessions,
                        currentPointerY = currentPointerY,
                        subIntervalMins = subIntervalMins,
                    )

                    TimelineTrackCanvas(
                        startHour = startHour,
                        endHour = endHour,
                        hourHeightDp = effectiveHourHeightDp.value.toInt().coerceAtLeast(1),
                        hourYOffsets = hourYOffsets,
                        hourSlotHeights = hourSlotHeights,
                        zones = zones,
                        sessions = sessions,
                        currentPointerY = currentPointerY,
                        isToday = isToday,
                        isPastDate = isPastDate,
                        subIntervalMins = subIntervalMins,
                    )

                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = axisWidthDp + 2.dp, end = 4.dp)
                            .zIndex(2f),
                    ) {
                        val hourHeightPx = effectiveHourHeightPx
                        val snapIntervalMins = if (effectiveHourHeightDpValue >= 60) 10 else 15
                        val displayMode = when {
                            effectiveHourHeightDpValue < 35f -> SessionDisplayMode.Pill
                            effectiveHourHeightDpValue < 50f -> SessionDisplayMode.Compact
                            else -> SessionDisplayMode.Full
                        }

                        positionedSessions.forEach { posSession ->
                            val session = posSession.session
                            key(session.id) {
                                val totalCols = posSession.totalColumns
                                val colIndex = posSession.columnIndex

                                val colWidthDp = maxWidth / totalCols
                                val leftOffsetDp = colWidthDp * colIndex

                                val targetTopDp = contentTopPadding + ((session.startMinutes.toDouble() / 60.0) * effectiveHourHeightDpValue).dp
                                val minCardHeight = when (displayMode) {
                                    SessionDisplayMode.Pill -> 6.dp
                                    SessionDisplayMode.Compact -> 24.dp
                                    SessionDisplayMode.Full -> 54.dp
                                }
                                val sessionHeightDp = ((session.durationMinutes.toDouble() / 60.0) * effectiveHourHeightDpValue)
                                    .dp.coerceAtLeast(minCardHeight)

                                DraggableTimelineSessionCard(
                                    session = session,
                                    leftOffsetDp = leftOffsetDp,
                                    targetTopDp = targetTopDp,
                                    colWidthDp = colWidthDp,
                                    sessionHeightDp = sessionHeightDp,
                                    contentTopPadding = contentTopPadding,
                                    hourHeightPx = hourHeightPx,
                                    snapIntervalMins = snapIntervalMins,
                                    scrollState = scrollState,
                                    viewportHeightPx = viewportHeightPx,
                                    displayMode = displayMode,
                                    onSessionMoved = onSessionMoved,
                                    onSessionStatusToggle = onSessionStatusToggle,
                                    onSessionClick = onSessionClick,
                                )
                            }
                        }
                    }

                    currentPointerY?.let { pY ->
                        LivingTimePointer(
                            pointerY = pY,
                            currentTimeFormatted = currentTimeFormatted,
                            activePointerColor = activePointerColor,
                            pulseScale = pulseScale,
                            pulseAlpha = pulseAlpha,
                        )
                    }

                    AwanCloudsHorizon(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = contentTopPadding + totalTimelineHeight + 10.dp)
                            .zIndex(100f),
                    )
                }
            }
        }
    }
}
