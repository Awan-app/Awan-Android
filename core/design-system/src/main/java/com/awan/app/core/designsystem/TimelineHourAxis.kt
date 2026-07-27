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

private fun formatHourLabel(hour: Int): String = when {
    hour == 0  -> "12 AM"
    hour == 12 -> "12 PM"
    hour > 12  -> "${hour - 12} PM"
    else       -> "$hour AM"
}

private fun formatMinuteLabel(totalMinutes: Int): String {
    val h = totalMinutes / 60
    val m = totalMinutes % 60
    val displayH = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else   -> h
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
    subIntervalMins: Int = 0,
    modifier: Modifier = Modifier,
) {
    for (hour in startHour until endHour) {
        val hourTopY   = hourYOffsets[hour] ?: 0.dp
        val hourHeight = hourSlotHeights[hour] ?: 0.dp

        val overlappingZone    = zones.find { hour >= it.startHour && hour < it.endHour }
        val isMiddleCollapsed  = overlappingZone != null &&
                overlappingZone.isCollapsed && hour > overlappingZone.startHour
        if (isMiddleCollapsed) continue

        AwanText(
            text = formatHourLabel(hour),
            style = AwanTheme.typography.caption.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textSecondary,
            ),
            modifier = modifier
                .offset(x = 6.dp, y = hourTopY - 6.dp)
                .width(50.dp)
                .zIndex(2f),
        )

        if (subIntervalMins > 0 && hourHeight >= (60f / subIntervalMins * 14f).dp) {
            val step = subIntervalMins
            var subMin = step
            while (subMin < 60) {
                val subFraction    = subMin / 60f
                val subOffsetY     = hourTopY + hourHeight * subFraction
                val subAbsMinutes  = hour * 60 + subMin

                val fontSize   = if (step == 5) 7.5.sp else 8.5.sp
                val labelColor = if (step == 5) AwanTheme.colors.meta else AwanTheme.colors.textSecondary

                AwanText(
                    text = formatMinuteLabel(subAbsMinutes),
                    style = AwanTheme.typography.caption.copy(
                        fontSize   = fontSize,
                        fontWeight = FontWeight.Normal,
                        color      = labelColor,
                    ),
                    modifier = modifier
                        .offset(x = 20.dp, y = subOffsetY - 5.dp)
                        .width(50.dp)
                        .zIndex(2f),
                )
                subMin += step
            }
        }
    }
}
