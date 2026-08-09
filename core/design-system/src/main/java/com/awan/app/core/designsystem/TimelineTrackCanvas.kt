package com.awan.app.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
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
    subIntervalMins: Int = 0,
    modifier: Modifier = Modifier,
) {
    val hourLineColor = AwanTheme.colors.line
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .zIndex(1f),
    ) {
        val trackX      = if (isRtl) size.width - 52.dp.toPx() else 52.dp.toPx()
        val lineEnd     = if (isRtl) 0f else size.width
        val pointerY    = currentPointerY?.toPx()
        val defaultGray = hourLineColor
        val pastGray    = hourLineColor.copy(alpha = 0.55f)

        fun getTrackColorAtHour(hour: Int): Color {
            val hMins = hour * 60
            val insideZone = zones.find { hMins > it.startMinutes && hMins < it.endMinutes }
            if (insideZone != null) return insideZone.category.color

            val startZone = zones.find { hMins == it.startMinutes }
            if (startZone != null) return startZone.category.color

            val endZone = zones.find { hMins == it.endMinutes }
            if (endZone != null) return endZone.category.color

            val hourTopPx = (hourYOffsets[hour] ?: (hour * hourHeightDp).dp).toPx()
            return if (isPastDate || (isToday && pointerY != null && hourTopPx <= pointerY))
                pastGray
            else
                defaultGray
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

            drawLine(
                color       = hourLineColor.copy(alpha = 0.70f),
                start       = Offset(trackX, hTop),
                end         = Offset(lineEnd, hTop),
                strokeWidth = 1.dp.toPx(),
            )

            when {
                isPastDate -> drawLine(
                    brush       = gradientBrush,
                    start       = Offset(trackX, hTop),
                    end         = Offset(trackX, hBottom),
                    strokeWidth = 3.dp.toPx(),
                )
                !isToday -> drawLine(
                    brush       = gradientBrush,
                    start       = Offset(trackX, hTop),
                    end         = Offset(trackX, hBottom),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect  = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                )
                else -> {
                    if (pointerY != null && pointerY >= hTop && pointerY <= hBottom) {
                        drawLine(gradientBrush, Offset(trackX, hTop), Offset(trackX, pointerY), 3.dp.toPx())
                        drawLine(
                            gradientBrush,
                            Offset(trackX, pointerY),
                            Offset(trackX, hBottom),
                            2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        )
                    } else if (pointerY != null && hBottom <= pointerY) {
                        drawLine(gradientBrush, Offset(trackX, hTop), Offset(trackX, hBottom), 3.dp.toPx())
                    } else {
                        drawLine(
                            gradientBrush,
                            Offset(trackX, hTop),
                            Offset(trackX, hBottom),
                            2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                        )
                    }
                }
            }

            if (subIntervalMins > 0 && hHeight > 0f) {
                val step = subIntervalMins
                val tickAlpha  = if (step == 5) 0x22 else 0x44
                val tickColor  = Color(tickAlpha shl 24 or 0xCBD5E1)
                val tickWidth  = if (step == 5) 5.dp.toPx() else 8.dp.toPx()
                val tickStroke = if (step == 5) 0.8.dp.toPx() else 1.dp.toPx()

                var subMin = step
                while (subMin < 60) {
                    val isMidHour = (subMin == 30)
                    val subY      = hTop + hHeight * (subMin / 60f)

                    if (isMidHour && step == 10) {
                        drawLine(
                            color       = Color(0xFFE2E8F0).copy(alpha = 0.45f),
                            start       = Offset(trackX, subY),
                            end         = Offset(lineEnd, subY),
                            strokeWidth = 0.8.dp.toPx(),
                            pathEffect  = PathEffect.dashPathEffect(floatArrayOf(6f, 8f), 0f),
                        )
                    } else {
                        val tickEnd = if (isRtl) trackX - tickWidth else trackX + tickWidth
                        drawLine(
                            color       = tickColor,
                            start       = Offset(trackX, subY),
                            end         = Offset(tickEnd, subY),
                            strokeWidth = tickStroke,
                        )
                    }
                    subMin += step
                }
            }
        }
    }
}
