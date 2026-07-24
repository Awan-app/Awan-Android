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

/**
 * Formats a full hour (e.g., 14) → "2 PM"
 */
private fun formatHourLabel(hour: Int): String = when {
    hour == 0  -> "12 AM"
    hour == 12 -> "12 PM"
    hour > 12  -> "${hour - 12} PM"
    else       -> "$hour AM"
}

/**
 * Formats an absolute minute value (e.g., 870 = 14h 30m) → "2:30"
 */
private fun formatMinuteLabel(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    val displayH = when {
        h == 0  -> 12
        h > 12  -> h - 12
    else    -> h
    }
    return "$displayH:${m.toString().padStart(2, '0')}"
}

@Composable
fun TimelineHourAxis(
    startHour: Int,
    endHour: Int,
    hourYOffsets: Map<Int, Dp>,
    hourSlotHeights: Map<Int, Dp>,
    zones: List<ScheduleZone>,
    sessions: List<ScheduleSession>,
    currentPointerY: Dp?,
    modifier: Modifier = Modifier,
) {
    /** Hours that hold more than one session — these get 10-min sub-ticks */
    val multiSessionHours = buildSet<Int> {
        zones.forEach { zone ->
            val count = sessions.count { it.zoneId == zone.id }
            if (count > 1) {
                for (h in zone.startHour until zone.endHour) add(h)
            }
        }
    }

    for (hour in startHour until endHour) {
        val hourTopY      = hourYOffsets[hour] ?: 0.dp
        val hourHeight    = hourSlotHeights[hour] ?: 0.dp
        val overlappingZone = zones.find { hour >= it.startHour && hour < it.endHour }
        val isMiddleCollapsed = overlappingZone != null &&
                overlappingZone.isCollapsed && hour > overlappingZone.startHour
        val isNearPointer = currentPointerY != null &&
                abs(currentPointerY.value - hourTopY.value) < 14f

        if (isMiddleCollapsed) continue

        if (!isNearPointer) {
            // — Primary hour label —
            AwanText(
                text = formatHourLabel(hour),
                style = AwanTheme.typography.caption.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                ),
                modifier = modifier
                    .offset(x = 0.dp, y = hourTopY)
                    .width(60.dp)
                    .zIndex(2f),
            )
        }

        // — 10-min sub-ticks for multi-session hours —
        if (multiSessionHours.contains(hour) && hourHeight > 80.dp) {
            val minutesPerPx = if (hourHeight.value > 0f) 60f / hourHeight.value else 0f
            for (subMin in 10..50 step 10) {
                val subFraction   = subMin / 60f
                val subOffsetY    = hourTopY + hourHeight * subFraction
                val subAbsMinutes = hour * 60 + subMin
                val isNearSubPointer = currentPointerY != null &&
                        abs(currentPointerY.value - subOffsetY.value) < 12f

                if (isNearSubPointer) continue

                AwanText(
                    text = formatMinuteLabel(subAbsMinutes),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF94A3B8),
                    ),
                    modifier = modifier
                        .offset(x = 2.dp, y = subOffsetY)
                        .width(60.dp)
                        .zIndex(2f),
                )
            }
        }
    }
}
