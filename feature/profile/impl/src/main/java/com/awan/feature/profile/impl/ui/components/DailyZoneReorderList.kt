package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.toColor
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RowGap = 8.dp
private val RestElevation = 3.dp
private val LiftElevation = 10.dp
private const val LIFT_SCALE_DELTA = 0.03f
private const val LIFT_TILT_DEGREES = -2f

@Composable
fun DailyZoneReorderList(
    zones: List<DailyZone>,
    onOpen: (DailyZone) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)
    val zoneCount = zones.size
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var slotHeight by remember { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val settleSpec = AwanTheme.motion.settle.spec<Float>()

    val target = draggingIndex?.let { from ->
        if (slotHeight == 0) from else (from + (dragOffset / slotHeight).roundToInt()).coerceIn(zones.indices)
    }

    LaunchedEffect(target) {
        if (target != null && !settling) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    val cardShape = AwanTheme.shapes.card
    val reduced = reducedMotion()

    Column(modifier.fillMaxWidth()) {
        key(zones.joinToString(",") { it.id ?: it.name }) {
            zones.forEachIndexed { index, zone ->
                val from = draggingIndex
                val isDragging = from == index
                val shift = when {
                    from == null || target == null -> 0f
                    index == from -> 0f
                    from < target && index in (from + 1)..target -> -slotHeight.toFloat()
                    from > target && index in target until from -> slotHeight.toFloat()
                    else -> 0f
                }
                val animatedShift by animateFloatAsState(
                    targetValue = shift,
                    animationSpec = AwanTheme.motion.settle.spec(),
                    label = "zoneShift",
                )
                val liftTarget by animateFloatAsState(
                    targetValue = if (isDragging && !settling) 1f else 0f,
                    animationSpec = settleSpec,
                    label = "zoneLift",
                )
                val lift = liftTarget.coerceAtLeast(0f)

                val endDrag = {
                    val f = draggingIndex
                    if (f != null && slotHeight > 0) {
                        val t = (f + (dragOffset / slotHeight).roundToInt()).coerceIn(0, zoneCount - 1)
                        settling = true
                        scope.launch {
                            animate(dragOffset, (t - f) * slotHeight.toFloat(), animationSpec = settleSpec) { v, _ ->
                                dragOffset = v
                            }
                            if (f != t) currentOnReorder(f, t)
                            draggingIndex = null
                            dragOffset = 0f
                            settling = false
                        }
                    } else {
                        draggingIndex = null
                        dragOffset = 0f
                    }
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                val startDrag = {
                    if (!settling) {
                        draggingIndex = index
                        dragOffset = 0f
                        haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { if (it.height > 0) slotHeight = it.height }
                        .zIndex(if (isDragging) 1f else 0f)
                        .padding(bottom = RowGap)
                        .graphicsLayer {
                            translationY = if (isDragging) dragOffset else animatedShift
                            scaleX = 1f + LIFT_SCALE_DELTA * lift
                            scaleY = 1f + LIFT_SCALE_DELTA * lift
                            rotationZ = if (reduced) 0f else LIFT_TILT_DEGREES * lift
                            shadowElevation = (RestElevation + (LiftElevation - RestElevation) * lift).toPx()
                            shape = cardShape
                            clip = false
                        }
                        .pointerInput(index, zones.size) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { startDrag() },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                },
                                onDragEnd = { endDrag() },
                                onDragCancel = { endDrag() },
                            )
                        }
                ) {
                    DailyZoneRow(
                        zone = zone,
                        onClick = { onOpen(zone) },
                        dragHandle = {
                            ZoneDragHandle(
                                color = zone.color.toColor(),
                                modifier = Modifier
                                    .pointerInput(index, zones.size) {
                                        detectDragGestures(
                                            onDragStart = { startDrag() },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                dragOffset += amount.y
                                            },
                                            onDragEnd = { endDrag() },
                                            onDragCancel = { endDrag() },
                                        )
                                    },
                            )
                        },
                    )
                }
            }
        }
    }
}
