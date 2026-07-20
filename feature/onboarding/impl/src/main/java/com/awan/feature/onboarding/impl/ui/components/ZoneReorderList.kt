package com.awan.feature.onboarding.impl.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.Zone
import com.awan.feature.onboarding.impl.R
import kotlin.math.roundToInt

private val RowGap = 8.dp
private const val LIFT_SCALE = 1.04f

/**
 * The zone list. Rows reorder by dragging the handle, or by long-pressing the row itself; the
 * equivalent move actions are exposed to accessibility services, which cannot drag.
 */
@Composable
fun ZoneReorderList(
    zones: List<Zone>,
    overlappingIds: Set<String>,
    onToggle: (String) -> Unit,
    onOpen: (Zone) -> Unit,
    onReorder: (Int, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val currentOnReorder by rememberUpdatedState(onReorder)
    val zoneCount = zones.size
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var slotHeight by remember { mutableIntStateOf(0) }

    val target = draggingIndex?.let { from ->
        if (slotHeight == 0) from else (from + (dragOffset / slotHeight).roundToInt()).coerceIn(zones.indices)
    }

    LaunchedEffect(target) {
        if (target != null) haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    val moveUp = stringResource(R.string.onboarding_zone_move_up)
    val moveDown = stringResource(R.string.onboarding_zone_move_down)
    val cardShape = AwanTheme.shapes.card

    val reduced = reducedMotion()
    val nudge = remember { Animatable(0f) }
    val nudgeSpec = AwanTheme.motion.playful.spec<Float>()
    LaunchedEffect(Unit) {
        if (!reduced) {
            nudge.animateTo(6f, nudgeSpec)
            nudge.animateTo(0f, nudgeSpec)
        }
    }

    Column(modifier.fillMaxWidth()) {
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

            // pointerInput caches its block, so these must read live state rather than close over
            // values computed during the composition that created them.
            val endDrag = {
                val f = draggingIndex
                if (f != null && slotHeight > 0) {
                    val t = (f + (dragOffset / slotHeight).roundToInt()).coerceIn(0, zoneCount - 1)
                    if (f != t) currentOnReorder(f, t)
                }
                draggingIndex = null
                dragOffset = 0f
                haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            }
            val startDrag = {
                draggingIndex = index
                dragOffset = 0f
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { if (it.height > 0) slotHeight = it.height }
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffset else animatedShift
                        scaleX = if (isDragging) LIFT_SCALE else 1f
                        scaleY = if (isDragging) LIFT_SCALE else 1f
                        shadowElevation = if (isDragging) 12.dp.toPx() else 0f
                        shape = cardShape
                        clip = false
                    }
                    .padding(bottom = RowGap)
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
                    .semantics {
                        customActions = buildList {
                            if (index > 0) {
                                add(CustomAccessibilityAction(moveUp) { onReorder(index, index - 1); true })
                            }
                            if (index < zones.lastIndex) {
                                add(CustomAccessibilityAction(moveDown) { onReorder(index, index + 1); true })
                            }
                        }
                    },
            ) {
                ZoneRow(
                    zone = zone,
                    overlapping = zone.id in overlappingIds,
                    onToggle = {
                        haptics.performHapticFeedback(
                            if (zone.isEnabled) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
                        )
                        onToggle(zone.id)
                    },
                    onClick = { onOpen(zone) },
                    dragHandle = {
                        val handleLabel = stringResource(R.string.onboarding_zone_drag_handle, zone.name)
                        ZoneDragHandle(
                            color = Color(zone.colorArgb),
                            modifier = Modifier
                                .semantics { contentDescription = handleLabel }
                                .graphicsLayer { if (index == 0) translationX = nudge.value }
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
