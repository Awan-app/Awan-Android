package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

import java.time.LocalDate

@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit,
    onNextWeek: () -> Unit,
    onPreviousWeek: () -> Unit,
    modifier: Modifier = Modifier,
    assignedDays: Set<DayOfWeek> = emptySet(),
    dayColors: Map<DayOfWeek, Color> = emptyMap(),
    specialDates: Map<String, Color> = emptyMap(),
    showTodayIndicator: Boolean = true,
    today: LocalDate = LocalDate.now(),
    referenceDate: LocalDate = LocalDate.now(),
    selectedDates: Set<String> = emptySet(),
    isTodayOnly: Boolean = false
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AwanIconButton(
            onClick = onPreviousWeek,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val currentWeekStart = referenceDate.minusDays((referenceDate.dayOfWeek.value.toLong() - 1))

            DayOfWeek.entries.forEach { day ->
                val dateForDay = currentWeekStart.plusDays(day.ordinal.toLong())
                val isSelected = if (isTodayOnly) {
                    selectedDates.contains(dateForDay.toString())
                } else {
                    selectedDays.contains(day)
                }
                val isAssigned = assignedDays.contains(day)
                val isToday = if (showTodayIndicator) {
                    DailyZonesHelper.isToday(day, today) && referenceDate.year == today.year && referenceDate.dayOfYear == today.dayOfYear
                } else false
                val templateColor = specialDates[dateForDay.toString()] ?: dayColors[day]
                
                val dayOfMonth = dateForDay.dayOfMonth.toString()
                val fullDayName = stringResource(DailyZonesHelper.getDayNameRes(day))

                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                val rimDepth = 4.dp
                val rimColor = AwanTheme.colors.line
                val surface = AwanTheme.colors.surface

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { onDaySelected(day) }
                        )
                ) {
                    if (isToday) {
                        AwanText(
                            text = "Today",
                            style = AwanTheme.styles.captionText.copy(
                                color = AwanTheme.colors.sky,
                                textStyle = AwanTheme.styles.captionText.textStyle.copy(
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Box(modifier = Modifier.size(38.dp, 44.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = if (isPressed) rimDepth else 0.dp)
                                .background(
                                    when {
                                        isSelected || isToday -> AwanTheme.colors.sky.copy(alpha = 0.5f)
                                        templateColor != null -> templateColor.copy(alpha = 0.3f)
                                        isAssigned && !isTodayOnly -> AwanTheme.colors.disabledSurface.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    },
                                    RoundedCornerShape(12.dp)
                                )
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = if (isPressed && !isSelected) 0.dp else rimDepth)
                                .background(
                                    color = when {
                                        isSelected -> AwanTheme.colors.sky
                                        isToday -> AwanTheme.colors.sky.copy(alpha = 0.2f).compositeOver(surface)
                                        templateColor != null -> templateColor.copy(alpha = 0.12f).compositeOver(surface)
                                        isAssigned && !isTodayOnly -> AwanTheme.colors.disabledSurface
                                        else -> surface
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isSelected || isToday) 2.dp else 1.dp,
                                    color = when {
                                        isSelected -> AwanTheme.colors.sky
                                        isToday -> AwanTheme.colors.sky
                                        templateColor != null -> templateColor.copy(alpha = 0.6f).compositeOver(surface)
                                        isAssigned && !isTodayOnly -> AwanTheme.colors.line.copy(alpha = 0.5f)
                                        else -> Color.Transparent // No border for empty days
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AwanText(
                                text = dayOfMonth,
                                style = AwanTheme.styles.bodyText.copy(
                                    textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = when {
                                        isSelected -> Color.White
                                        isToday -> AwanTheme.colors.sky
                                        isAssigned && !isTodayOnly -> AwanTheme.colors.disabledContent
                                        templateColor != null -> templateColor
                                        else -> AwanTheme.colors.textPrimary
                                    }
                                )
                            )
                        }
                    }

                    AwanText(
                        text = fullDayName.take(3),
                        style = AwanTheme.styles.captionText.copy(
                            textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp),
                            color = when {
                                isSelected -> AwanTheme.colors.sky
                                isToday -> AwanTheme.colors.sky
                                isAssigned -> AwanTheme.colors.disabledContent
                                templateColor != null -> templateColor
                                else -> AwanTheme.colors.textSecondary
                            }
                        )
                    )
                }
            }
        }

        AwanIconButton(
            onClick = onNextWeek,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
