package com.awan.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun TimelineTrackCanvas(
    startHour: Int,
    endHour: Int,
    hourHeightDp: Int,
    hourYOffsets: Map<Int, Dp>,
    hourSlotHeights: Map<Int, Dp>,
    zones: List<ScheduleZone>,
    sessions: List<ScheduleSession>,
    currentPointerY: Dp?,
    isToday: Boolean = true,
    isPastDate: Boolean = false,
    modifier: Modifier = Modifier,
) {
    /** Hours that hold more than one session — these get 10-min tick marks */
    val multiSessionHours = buildSet<Int> {
        zones.forEach { zone ->
            val count = sessions.count { it.zoneId == zone.id }
            if (count > 1) {
                for (h in zone.startHour until zone.endHour) add(h)
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1f)
    ) {
        val trackX    = 52.dp.toPx()
        val pointerY  = currentPointerY?.toPx()
        val defaultGray = Color(0xFFCBD5E1)
        val pastGray    = Color(0xFF94A3B8)

        // sub-tick styling
        val subTickColor = Color(0x33CBD5E1)   // very subtle – 20% opacity
        val subTickWidth = 6.dp.toPx()         // short horizontal mark
        val subTickStroke = 1.dp.toPx()

        fun getTrackColorAtHour(hour: Int): Color {
            val insideZone = zones.find { hour > it.startHour && hour < it.endHour }
            if (insideZone != null) return insideZone.category.color

            val startZone = zones.find { hour == it.startHour }
            if (startZone != null) return startZone.category.color

            val endZone = zones.find { hour == it.endHour }
            if (endZone != null) return endZone.category.color

            val hourTopDp = hourYOffsets[hour] ?: (hour * hourHeightDp).dp
            val hourTopPx = hourTopDp.toPx()
            return if (isPastDate || (isToday && pointerY != null && hourTopPx <= pointerY)) pastGray else defaultGray
        }

        for (h in startHour until endHour) {
            val hTopDp    = hourYOffsets[h] ?: (h * hourHeightDp).dp
            val hBottomDp = hourYOffsets[h + 1] ?: (hTopDp + hourHeightDp.dp)
            val hTop      = hTopDp.toPx()
            val hBottom   = hBottomDp.toPx()
            val hHeight   = hBottom - hTop

            val colorTop    = getTrackColorAtHour(h)
            val colorBottom = getTrackColorAtHour(h + 1)

            val gradientBrush = Brush.verticalGradient(
                colors = listOf(colorTop, colorBottom),
                startY = hTop,
                endY   = hBottom,
            )

            // ── Main vertical track line ──────────────────────────────────────
            if (isPastDate) {
                drawLine(
                    brush = gradientBrush,
                    start = Offset(trackX, hTop),
                    end   = Offset(trackX, hBottom),
                    strokeWidth = 3.dp.toPx(),
                )
            } else if (!isToday) {
                drawLine(
                    brush = gradientBrush,
                    start = Offset(trackX, hTop),
                    end   = Offset(trackX, hBottom),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
            } else {
                if (pointerY != null && pointerY >= hTop && pointerY <= hBottom) {
                    drawLine(
                        brush = gradientBrush,
                        start = Offset(trackX, hTop),
                        end   = Offset(trackX, pointerY),
                        strokeWidth = 3.dp.toPx(),
                    )
                    drawLine(
                        brush = gradientBrush,
                        start = Offset(trackX, pointerY),
                        end   = Offset(trackX, hBottom),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    )
                } else if (pointerY != null && hBottom <= pointerY) {
                    drawLine(
                        brush = gradientBrush,
                        start = Offset(trackX, hTop),
                        end   = Offset(trackX, hBottom),
                        strokeWidth = 3.dp.toPx(),
                    )
                } else {
                    drawLine(
                        brush = gradientBrush,
                        start = Offset(trackX, hTop),
                        end   = Offset(trackX, hBottom),
                        strokeWidth = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                    )
                }
            }

            // ── 10-min sub-tick marks for multi-session hours ─────────────────
            if (multiSessionHours.contains(h) && hHeight > 80f) {
                for (subMin in 10..50 step 10) {
                    val subFraction = subMin / 60f
                    val subY        = hTop + hHeight * subFraction

                    // Subtle horizontal tick to the right of the track line
                    drawLine(
                        color       = subTickColor,
                        start       = Offset(trackX, subY),
                        end         = Offset(trackX + subTickWidth, subY),
                        strokeWidth = subTickStroke,
                    )
                }
            }
        }
    }
}
