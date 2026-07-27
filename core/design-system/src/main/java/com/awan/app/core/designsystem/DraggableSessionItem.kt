package com.awan.app.core.designsystem

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex

@Composable
fun DraggableSessionItem(
    session: ScheduleSession,
    index: Int,
    sessionsCount: Int,
    draggingIndex: Int?,
    targetIndex: Int?,
    slotHeight: Int,
    dragOffset: Float,
    settling: Boolean,
    onStartDrag: () -> Unit,
    onDrag: (Float) -> Unit,
    onEndDrag: () -> Unit,
    onSessionStatusToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDragging = draggingIndex == index
    val shift = when {
        draggingIndex == null || targetIndex == null -> 0f
        index == draggingIndex -> 0f
        draggingIndex < targetIndex && index in (draggingIndex + 1)..targetIndex -> -slotHeight.toFloat()
        draggingIndex > targetIndex && index in targetIndex until draggingIndex -> slotHeight.toFloat()
        else -> 0f
    }
    val animatedShift by animateFloatAsState(
        targetValue = shift,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sessionShift",
    )
    val liftTarget by animateFloatAsState(
        targetValue = if (isDragging && !settling) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "sessionLift",
    )
    val lift = liftTarget.coerceAtLeast(0f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                translationY = if (isDragging) dragOffset else animatedShift
                scaleX = 1f + 0.03f * lift
                scaleY = 1f + 0.03f * lift
                rotationZ = -2f * lift
                clip = false
            }
            .pointerInput(index, sessionsCount) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { onStartDrag() },
                    onDrag = { change, amount ->
                        change.consume()
                        onDrag(amount.y)
                    },
                    onDragEnd = { onEndDrag() },
                    onDragCancel = { onEndDrag() },
                )
            },
    ) {
        AwanScheduleTaskCard(
            title = session.taskTitle,
            timeRange = formatMinutesToRange(session.startMinutes, session.durationMinutes),
            category = session.category,
            status = session.status,
            points = session.points,
            isDragging = isDragging,
            onStatusToggle = { onSessionStatusToggle(session.id) },
        )
    }
}
