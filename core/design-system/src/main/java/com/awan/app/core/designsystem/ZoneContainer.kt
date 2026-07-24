package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ZoneContainer(
    zone: ScheduleZone,
    sessions: List<ScheduleSession>,
    startHourMinutes: Int,
    endHourMinutes: Int,
    hourHeightDp: Int,
    intervalMinutes: Int,
    onToggleCollapse: () -> Unit,
    onSessionStatusToggle: (String) -> Unit,
    onSessionMoved: (String, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var slotHeight by remember { mutableIntStateOf(0) }
    var settling by remember { mutableStateOf(false) }

    val sessionCount = sessions.size
    val targetIndex = draggingIndex?.let { from ->
        if (slotHeight == 0) from else (from + (dragOffset / slotHeight).roundToInt()).coerceIn(sessions.indices)
    }

    LaunchedEffect(targetIndex) {
        if (targetIndex != null && !settling) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    val completedCount = sessions.count { it.status == TaskStatus.Completed }
    val totalCount = sessions.size
    val isAllCompleted = totalCount > 0 && completedCount == totalCount

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, spotColor = zone.category.color.copy(alpha = 0.25f))
            .clip(shape)
            .background(Color(0xFFFAFAFC))
            .border(1.dp, Color(0xFFE2E8F0), shape),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(zone.category.color)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleCollapse,
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(zone.category.color, CircleShape),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AwanText(
                            text = zone.category.name,
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = zone.category.color,
                            ),
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        val badgeShape = RoundedCornerShape(99.dp)
                        val badgeBg = if (isAllCompleted) Color(0xFFDCFCE7) else Color(0xFFF1F5F9)
                        val badgeBorder = if (isAllCompleted) Color(0xFF86EFAC) else Color(0xFFE2E8F0)
                        val badgeText = if (isAllCompleted) "$completedCount/$totalCount done" else "$completedCount/$totalCount tasks"
                        val badgeColor = if (isAllCompleted) Color(0xFF15803D) else Color(0xFF64748B)

                        Box(
                            modifier = Modifier
                                .clip(badgeShape)
                                .background(badgeBg)
                                .border(1.dp, badgeBorder, badgeShape)
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AwanText(
                                text = badgeText,
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                ),
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val intervalShape = RoundedCornerShape(12.dp)
                        Box(
                            modifier = Modifier
                                .clip(intervalShape)
                                .background(zone.category.color.copy(alpha = 0.10f))
                                .border(1.dp, zone.category.color.copy(alpha = 0.25f), intervalShape)
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            AwanText(
                                text = "${formatHourTo12h(zone.startHour)} - ${formatHourTo12h(zone.endHour)}",
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = zone.category.color,
                                ),
                            )
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onToggleCollapse,
                                )
                                .padding(4.dp)
                        ) {
                            AwanText(
                                text = if (zone.isCollapsed) "▼" else "▲",
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.5.sp,
                                    color = zone.category.color,
                                ),
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = !zone.isCollapsed,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (sessions.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = "No scheduled sessions in this zone",
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                    ),
                                )
                            }
                        } else {
                            sessions.forEachIndexed { index, session ->
                                val endDrag = {
                                    val f = draggingIndex
                                    if (f != null && slotHeight > 0) {
                                        val t = (f + (dragOffset / slotHeight).roundToInt()).coerceIn(0, sessionCount - 1)
                                        settling = true
                                        scope.launch {
                                            animate(
                                                initialValue = dragOffset,
                                                targetValue = (t - f) * slotHeight.toFloat(),
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                    stiffness = Spring.StiffnessLow,
                                                ),
                                            ) { v, _ ->
                                                dragOffset = v
                                            }
                                            if (f != t && t in sessions.indices) {
                                                val targetSession = sessions[t]
                                                onSessionMoved(session.id, targetSession.startMinutes)
                                            }
                                            draggingIndex = null
                                            dragOffset = 0f
                                            settling = false
                                        }
                                    } else {
                                        draggingIndex = null
                                        dragOffset = 0f
                                    }
                                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                }

                                val startDrag = {
                                    if (!settling) {
                                        draggingIndex = index
                                        dragOffset = 0f
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    }
                                }

                                DraggableSessionItem(
                                    session = session,
                                    index = index,
                                    sessionsCount = sessions.size,
                                    draggingIndex = draggingIndex,
                                    targetIndex = targetIndex,
                                    slotHeight = slotHeight,
                                    dragOffset = dragOffset,
                                    settling = settling,
                                    onStartDrag = startDrag,
                                    onDrag = { deltaY -> dragOffset += deltaY },
                                    onEndDrag = endDrag,
                                    onSessionStatusToggle = onSessionStatusToggle,
                                    modifier = Modifier.onSizeChanged { if (it.height > 0) slotHeight = it.height },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
