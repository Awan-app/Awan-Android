package com.awan.app.core.designsystem

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlin.math.abs

@Composable
fun TimelineHourAxis(
    startHour: Int,
    endHour: Int,
    hourYOffsets: Map<Int, Dp>,
    zones: List<ScheduleZone>,
    currentPointerY: Dp?,
    modifier: Modifier = Modifier,
) {
    for (hour in startHour until endHour) {
        val hourTopY = hourYOffsets[hour] ?: 0.dp
        val overlappingZone = zones.find { hour >= it.startHour && hour < it.endHour }
        val isMiddleCollapsedHour = overlappingZone != null && overlappingZone.isCollapsed && hour > overlappingZone.startHour
        val isNearCurrentPointer = currentPointerY != null && abs(currentPointerY.value - hourTopY.value) < 14f

        if (!isMiddleCollapsedHour && !isNearCurrentPointer) {
            val amPmLabel = when {
                hour == 0 -> "12 AM"
                hour == 12 -> "12 PM"
                hour > 12 -> "${hour - 12} PM"
                else -> "$hour AM"
            }
            AwanText(
                text = amPmLabel,
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                ),
                modifier = modifier
                    .offset(x = 0.dp, y = hourTopY)
                    .width(44.dp)
                    .zIndex(2f),
            )
        }
    }
}
