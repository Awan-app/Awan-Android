package com.awan.feature.profile.impl.ui.components

import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun ZoneTimelineItem(
    zone: DailyZone,
    isLast: Boolean,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(64.dp)
        ) {
            AwanText(
                text = DailyZonesHelper.formatTime12h(zone.startTime),
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.styles.captionText.textStyle.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .width(2.dp)
                    .background(AwanTheme.colors.line)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(zone.color.toColor())
                        .align(Alignment.TopCenter)
                        .border(1.5.dp, AwanTheme.colors.background, CircleShape)
                )
            }

            if (isLast) {
                AwanText(
                    text = DailyZonesHelper.formatTime12h(zone.endTime),
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    )
                )
            }
        }

        // Zone card body
        ZoneCardBody(
            zone = zone,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 12.dp),
            onClick = onClick
        )
    }
}

@Composable
fun ZoneCardBody(
    zone: DailyZone,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val zoneColor = zone.color.toColor()
    // Opaque background to prevent the card rim/shadow from bleeding through.
    val backgroundColor = zoneColor
        .copy(alpha = AwanTheme.colors.zoneCardAlpha)
        .compositeOver(AwanTheme.colors.surface)

    AwanCard(
        modifier = modifier,
        contentPadding = PaddingValues(0.dp),
        background = backgroundColor,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(zoneColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = zone.name.ifBlank { "New Zone" },
                    style = AwanTheme.styles.bodyText.copy(
                        textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold),
                        color = if (zone.name.isBlank()) AwanTheme.colors.textSecondary else AwanTheme.colors.textPrimary
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = AwanTheme.colors.textSecondary
                    )
                    AwanText(
                        text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                    )
                }
            }
        }
    }
}

private const val PRESS_SCALE = 0.98f
private val TimeIndent = 25.dp

@Composable
fun DailyZoneRow(
    zone: DailyZone,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandle: @Composable () -> Unit,
) {
    val zoneColor = zone.color.toColor()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val reduced = reducedMotion()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed && !reduced) PRESS_SCALE else 1f,
        animationSpec = AwanTheme.motion.settle.spec(),
        label = "zonePress",
    )

    val backgroundColor = zoneColor
        .copy(alpha = AwanTheme.colors.zoneCardAlpha)
        .compositeOver(AwanTheme.colors.surface)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .clip(AwanTheme.shapes.card)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            dragHandle()
            AwanText(
                zone.name,
                style = AwanTheme.styles.headingText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = TimeIndent),
        ) {
            AwanText(
                text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                style = AwanTheme.styles.skipLink,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = zoneColor,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private val RowGap = 8.dp
private val RestElevation = 3.dp
private val LiftElevation = 10.dp
private const val LIFT_SCALE_DELTA = 0.03f
private const val LIFT_TILT_DEGREES = -2f
private val LiftHeadroom = 4.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneEditSheet(
    zone: DailyZone,
    onDismiss: () -> Unit,
    onConfirm: (DailyZone) -> Unit,
    onDelete: (() -> Unit)? = null,
    isNew: Boolean = false
) {
    var name by remember(zone) { mutableStateOf(zone.name) }
    var startTime by remember(zone) { mutableStateOf(zone.startTime) }
    var endTime by remember(zone) { mutableStateOf(zone.endTime) }
    var color by remember(zone) { mutableStateOf(if (zone.color.startsWith("#")) zone.color else "#2EAAFF") }

    val startMins = DailyZonesHelper.parseTimeToMinutes(startTime)
    val endMins = DailyZonesHelper.parseTimeToMinutes(endTime)

    val startState = rememberTimePickerState(
        initialHour = (startMins / 60).coerceIn(0, 23),
        initialMinute = (startMins % 60).coerceIn(0, 59)
    )
    val endState = rememberTimePickerState(
        initialHour = (endMins / 60).coerceIn(0, 23),
        initialMinute = (endMins % 60).coerceIn(0, 59)
    )

    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (isNew) "Add Zone" else "Edit Zone",
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(onClick = onDismiss, contentDescription = "Close") {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            // Name input
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(text = "Zone Name", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "e.g. Deep Work, Workout",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Time Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TimeInputBox(
                    label = "Start Time",
                    time = startTime,
                    onClick = { showStartTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
                TimeInputBox(
                    label = "End Time",
                    time = endTime,
                    onClick = { showEndTimePicker = true },
                    modifier = Modifier.weight(1f)
                )
            }

            // Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(text = "Label Color", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    listOf("#2EAAFF", "#FF9F2E", "#AA2EFF", "#2EFFA3", "#FF2E63", "#FFEB3B", "#4CAF50").forEach { hex ->
                        val isSelected = color.equals(hex, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(hex.toColor())
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) AwanTheme.colors.textPrimary else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { color = hex }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanButton(
                    onClick = {
                        onConfirm(zone.copy(name = name, startTime = startTime, endTime = endTime, color = color))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AwanText(text = if (isNew) "Add Zone" else "Save Changes")
                }

                if (!isNew && onDelete != null) {
                    AwanButton(
                        onClick = onDelete,
                        modifier = Modifier.fillMaxWidth(),
                        variant = AwanButtonVariant.Secondary,
                        icon = Icons.Default.Delete
                    ) {
                        AwanText(text = "Delete Zone")
                    }
                }
            }
        }
    }

    if (showStartTimePicker) {
        TimePickerDialog(
            state = startState,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                startTime = String.format(Locale.US, "%02d:%02d", startState.hour, startState.minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        TimePickerDialog(
            state = endState,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                endTime = String.format(Locale.US, "%02d:%02d", endState.hour, endState.minute)
                showEndTimePicker = false
            }
        )
    }
}

@Composable
private fun TimeInputBox(
    label: String,
    time: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AwanText(text = label, style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.textSecondary))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp)),
            color = AwanTheme.colors.surface
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.AccessTime, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(20.dp))
                AwanText(text = DailyZonesHelper.formatTime12h(time), style = AwanTheme.styles.bodyText)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                AwanText(text = "Confirm", style = AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.sky))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Cancel", style = AwanTheme.styles.bodyText)
            }
        },
        text = {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                AwanTimePicker(state = state)
            }
        },
        containerColor = AwanTheme.colors.background
    )
}

@Composable
fun ZoneDragHandle(color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = 2.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        repeat(3) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                repeat(2) {
                    Box(Modifier.size(3.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

fun String?.toColor(): Color {
    if (this == null || this.isBlank()) return Color.Transparent
    return try {
        val colorString = if (!this.startsWith("#")) "#$this" else this
        Color(android.graphics.Color.parseColor(colorString))
    } catch (e: Exception) {
        Color.Transparent
    }
}
