package com.awan.app.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CalendarDay(
    val dayName: String,
    val dayNumber: Int,
    val isSelected: Boolean = false,
)

@Composable
fun AwanWeeklyCalendarStrip(
    headerTitle: String,
    days: List<CalendarDay>,
    onDaySelected: (CalendarDay) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        AwanText(
            text = headerTitle,
            style = AwanTheme.typography.title.copy(
                fontSize = 17.sp,
                color = Color(0xFF1E293B),
            ),
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            days.forEach { calendarDay ->
                CalendarDayItem(
                    calendarDay = calendarDay,
                    onClick = { onDaySelected(calendarDay) },
                )
            }
        }
    }
}

@Composable
private fun CalendarDayItem(
    calendarDay: CalendarDay,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = calendarDay.isSelected
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1.0f,
        animationSpec = tween(durationMillis = 200),
        label = "calendarScale",
    )

    val dayNameColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B),
        label = "dayNameColor",
    )

    val shape = RoundedCornerShape(14.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        AwanText(
            text = calendarDay.dayName,
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = dayNameColor,
            ),
        )

        Spacer(modifier = Modifier.height(4.dp))

        if (isSelected) {
            // 3D Pill button for selected day
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(46.dp)
                    .shadow(
                        elevation = 6.dp,
                        shape = shape,
                        spotColor = Color(0xFF2563EB),
                    )
                    .clip(shape)
                    .background(Color(0xFF2563EB))
                    .border(2.dp, Color(0xFF60A5FA), shape),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = calendarDay.dayNumber.toString(),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 18.sp,
                        color = Color.White,
                    ),
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(46.dp),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = calendarDay.dayNumber.toString(),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 17.sp,
                        color = Color(0xFF1E293B),
                    ),
                )
            }
        }
    }
}
