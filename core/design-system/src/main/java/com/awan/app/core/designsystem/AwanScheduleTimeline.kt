package com.awan.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

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
    onSessionMoved: (sessionId: String, newStartMinutes: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier,
) {
    val totalHours = (endHour - startHour).coerceAtLeast(1)
    val density = LocalDensity.current

    val measuredZoneHeights = remember(zones, sessions) { mutableStateMapOf<String, Dp>() }

    val hourSlotHeights = remember(zones, sessions, startHour, endHour, hourHeightDp, measuredZoneHeights.toMap()) {
        val heights = mutableMapOf<Int, Dp>()
        for (h in startHour until endHour) {
            val overlappingZone = zones.find { h >= it.startHour && h < it.endHour }
            if (overlappingZone != null) {
                if (overlappingZone.isCollapsed) {
                    heights[h] = if (h == overlappingZone.startHour) 48.dp else 0.dp
                } else {
                    val measuredHeight = measuredZoneHeights[overlappingZone.id]
                    val zoneDuration = (overlappingZone.endHour - overlappingZone.startHour).coerceAtLeast(1)
                    if (measuredHeight != null) {
                        val requiredPerHour = measuredHeight / zoneDuration
                        heights[h] = if (requiredPerHour > hourHeightDp.dp) requiredPerHour else hourHeightDp.dp
                    } else {
                        val zoneSessions = sessions.filter { it.zoneId == overlappingZone.id }
                        val taskCount = zoneSessions.size
                        val requiredDp = if (taskCount == 0) 80.dp else (96 + taskCount * 84).dp
                        val requiredPerHour = requiredDp / zoneDuration
                        heights[h] = if (requiredPerHour > hourHeightDp.dp) requiredPerHour else hourHeightDp.dp
                    }
                }
            } else {
                heights[h] = hourHeightDp.dp
            }
        }
        heights
    }

    val hourYOffsets = remember(hourSlotHeights, startHour, endHour) {
        val offsets = mutableMapOf<Int, Dp>()
        var currentY = 0.dp
        for (h in startHour..endHour) {
            offsets[h] = currentY
            if (h < endHour) {
                currentY += (hourSlotHeights[h] ?: hourHeightDp.dp)
            }
        }
        offsets
    }

    val totalTimelineHeight = hourYOffsets[endHour] ?: (totalHours * hourHeightDp).dp
    val scrollState = rememberScrollState()
    var hasInitialScrolled by remember { mutableStateOf(false) }

    LaunchedEffect(currentTimeMinutes > 0, isToday) {
        if (isToday && currentTimeMinutes > 0 && !hasInitialScrolled) {
            hasInitialScrolled = true
            val currentHour = currentTimeMinutes / 60
            val targetY = hourYOffsets[currentHour] ?: 0.dp
            scrollState.scrollTo(targetY.value.toInt())
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "livingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseScale",
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "pulseAlpha",
    )

    val activeZone = remember(zones, currentTimeMinutes) {
        zones.find { currentTimeMinutes in (it.startHour * 60)..(it.endHour * 60) }
    }
    val activePointerColor = activeZone?.category?.color ?: Color(0xFF64748B)

    val currentPointerY = remember(isToday, currentTimeMinutes, hourYOffsets, hourSlotHeights) {
        if (isToday && currentTimeFormatted.isNotBlank() && currentTimeMinutes in (startHour * 60)..(endHour * 60)) {
            val currentHour = currentTimeMinutes / 60
            val hourStartMins = currentHour * 60
            val minuteFraction = (currentTimeMinutes - hourStartMins).toFloat() / 60f
            val currentHourY = hourYOffsets[currentHour] ?: 0.dp
            val currentHourHeight = hourSlotHeights[currentHour] ?: hourHeightDp.dp
            currentHourY + (currentHourHeight * minuteFraction)
        } else {
            null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalTimelineHeight + 35.dp),
            ) {
                TimelineHourAxis(
                    startHour = startHour,
                    endHour = endHour,
                    hourYOffsets = hourYOffsets,
                    hourSlotHeights = hourSlotHeights,
                    zones = zones,
                    sessions = sessions,
                    currentPointerY = currentPointerY,
                )

                TimelineTrackCanvas(
                    startHour = startHour,
                    endHour = endHour,
                    hourHeightDp = hourHeightDp,
                    hourYOffsets = hourYOffsets,
                    hourSlotHeights = hourSlotHeights,
                    zones = zones,
                    sessions = sessions,
                    currentPointerY = currentPointerY,
                    isToday = isToday,
                    isPastDate = isPastDate,
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 68.dp),
                ) {
                    zones.forEach { zone ->
                        val zoneTopY = hourYOffsets[zone.startHour] ?: 0.dp
                        val zoneBottomY = hourYOffsets[zone.endHour] ?: (zoneTopY + 64.dp)
                        val zoneHeightDp = if (zone.isCollapsed) 48.dp else (zoneBottomY - zoneTopY).coerceAtLeast(64.dp)
                        val zoneSessions = sessions.filter { it.zoneId == zone.id }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = zoneTopY)
                                .heightIn(min = zoneHeightDp),
                        ) {
                            ZoneContainer(
                                zone = zone,
                                sessions = zoneSessions,
                                startHourMinutes = zone.startHour * 60,
                                endHourMinutes = zone.endHour * 60,
                                hourHeightDp = hourHeightDp,
                                intervalMinutes = intervalMinutes,
                                onToggleCollapse = { onToggleZoneCollapse(zone.id) },
                                onSessionStatusToggle = onSessionStatusToggle,
                                onSessionMoved = onSessionMoved,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onSizeChanged { size ->
                                        val measured = with(density) { size.height.toDp() }
                                        if (measured > (measuredZoneHeights[zone.id] ?: 0.dp)) {
                                            measuredZoneHeights[zone.id] = measured
                                        }
                                    },
                            )
                        }
                    }
                }

                currentPointerY?.let { pointerY ->
                    LivingTimePointer(
                        pointerY = pointerY,
                        currentTimeFormatted = currentTimeFormatted,
                        activePointerColor = activePointerColor,
                        pulseScale = pulseScale,
                        pulseAlpha = pulseAlpha,
                    )
                }

                AwanCloudsHorizon(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = totalTimelineHeight - 28.dp),
                )
            }
        }
    }
}
