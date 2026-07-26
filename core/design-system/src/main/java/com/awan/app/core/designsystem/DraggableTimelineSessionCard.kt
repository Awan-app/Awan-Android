package com.awan.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun DraggableTimelineSessionCard(
    session: ScheduleSession,
    leftOffsetDp: Dp,
    targetTopDp: Dp,
    colWidthDp: Dp,
    sessionHeightDp: Dp,
    contentTopPadding: Dp,
    hourHeightPx: Float,
    snapIntervalMins: Int,
    scrollState: ScrollState,
    viewportHeightPx: Float,
    onSessionMoved: (sessionId: String, newStartMinutes: Int) -> Unit,
    onSessionStatusToggle: (sessionId: String) -> Unit,
    displayMode: SessionDisplayMode = SessionDisplayMode.Full,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current

    var isDragging by remember { mutableStateOf(false) }
    var cumulativeDragPx by remember { mutableFloatStateOf(0f) }
    var touchOffsetInCardPx by remember { mutableFloatStateOf(0f) }

    val latestSessionStartMins = rememberUpdatedState(session.startMinutes)
    var dragStartMins by remember { mutableStateOf(0) }

    LaunchedEffect(isDragging) {
        if (!isDragging) return@LaunchedEffect
        val scrollThresholdPx = with(density) { 90.dp.toPx() }
        val maxScrollSpeedPx = with(density) { 14.dp.toPx() }

        while (isDragging) {
            val currentScroll = scrollState.value.toFloat()
            val visibleTop = currentScroll
            val visibleBottom = currentScroll + viewportHeightPx

            val cardTopPx = with(density) { contentTopPadding.toPx() } + (dragStartMins.toFloat() / 60f) * hourHeightPx
            val currentTouchY = cardTopPx + touchOffsetInCardPx + cumulativeDragPx

            val distFromTop = currentTouchY - visibleTop
            val distFromBottom = visibleBottom - currentTouchY

            var scrollDelta = 0f
            if (distFromTop < scrollThresholdPx && scrollState.value > 0) {
                val factor = (1f - (distFromTop / scrollThresholdPx).coerceIn(0f, 1f))
                scrollDelta = -maxScrollSpeedPx * factor
            } else if (distFromBottom < scrollThresholdPx && scrollState.value < scrollState.maxValue) {
                val factor = (1f - (distFromBottom / scrollThresholdPx).coerceIn(0f, 1f))
                scrollDelta = maxScrollSpeedPx * factor
            }

            if (scrollDelta != 0f) {
                val actualScrolled = scrollState.scrollBy(scrollDelta)
                cumulativeDragPx += actualScrolled
            }
            delay(16)
        }
    }

    var isJustMoved by remember(session.startMinutes) { mutableStateOf(true) }
    LaunchedEffect(session.startMinutes) {
        delay(40)
        isJustMoved = false
    }

    val moveAlpha by animateFloatAsState(
        targetValue = if (isJustMoved && !isDragging) 0.25f else 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "moveAlpha_${session.id}",
    )

    val moveScale by animateFloatAsState(
        targetValue = if (isDragging) 1.015f else if (isJustMoved) 0.93f else 1f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "moveScale_${session.id}",
    )

    val constantCategory = session.category

    val latestHourHeightPx = rememberUpdatedState(hourHeightPx)
    val latestSnapIntervalMins = rememberUpdatedState(snapIntervalMins)

    fun calcMovedMins(dragPx: Float): Int {
        val h = latestHourHeightPx.value
        val snap = latestSnapIntervalMins.value
        val rawDeltaMins = (dragPx / h) * 60f
        val snappedDelta = (rawDeltaMins / snap).roundToInt() * snap
        val maxStartMins = (24 * 60 - session.durationMinutes).coerceAtLeast(0)
        return (dragStartMins + snappedDelta).coerceIn(0, maxStartMins)
    }

    val liveTargetMins = if (isDragging) calcMovedMins(cumulativeDragPx) else session.startMinutes

    Box(
        modifier = modifier
            .offset(x = leftOffsetDp, y = targetTopDp)
            .offset {
                if (isDragging) IntOffset(0, cumulativeDragPx.roundToInt())
                else IntOffset.Zero
            }
            .width(colWidthDp - 3.dp)
            .height(sessionHeightDp)
            .zIndex(if (isDragging) 999f else 2f)
            .graphicsLayer {
                alpha = moveAlpha
                scaleX = moveScale
                scaleY = moveScale
                shadowElevation = 0f
                transformOrigin = TransformOrigin(0.5f, 0.5f)
                clip = false
            }
            .pointerInput(session.id) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        if (session.isFixed || session.status is TaskStatus.Fixed) {
                            return@detectDragGesturesAfterLongPress
                        }
                        dragStartMins = latestSessionStartMins.value
                        cumulativeDragPx = 0f
                        touchOffsetInCardPx = offset.y
                        isDragging = true
                        haptic.performHapticFeedback(
                            HapticFeedbackType.GestureThresholdActivate,
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        cumulativeDragPx += dragAmount.y
                    },
                    onDragEnd = {
                        val finalMins = calcMovedMins(cumulativeDragPx)
                        if (finalMins != dragStartMins) {
                            haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                            onSessionMoved(session.id, finalMins)
                        }
                        isDragging = false
                        cumulativeDragPx = 0f
                    },
                    onDragCancel = {
                        isDragging = false
                        cumulativeDragPx = 0f
                    },
                )
            },
    ) {
        AwanScheduleTaskCard(
            title = session.taskTitle,
            timeRange = if (isDragging)
                "⏱ ${formatTime(liveTargetMins)}"
            else
                formatMinutesToRange(session.startMinutes, session.durationMinutes),
            category = constantCategory,
            status = session.status,
            points = session.points,
            isDragging = isDragging,
            displayMode = displayMode,
            onStatusToggle = { onSessionStatusToggle(session.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(sessionHeightDp),
        )
    }
}
