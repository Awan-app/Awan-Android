package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun DaySelector(
    selectedDays: Set<DayOfWeek>,
    onDaySelected: (DayOfWeek) -> Unit,
    modifier: Modifier = Modifier,
    assignedDays: Set<DayOfWeek> = emptySet(),
    dayColors: Map<DayOfWeek, Color> = emptyMap(),
    showTodayIndicator: Boolean = true
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = selectedDays.contains(day)
            val isAssigned = assignedDays.contains(day)
            val isToday = if (showTodayIndicator) DailyZonesHelper.isToday(day) else false
            val templateColor = dayColors[day]
            val abbreviation = stringResource(DailyZonesHelper.getDayAbbreviationRes(day))

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
                        enabled = !isAssigned,
                        onClick = { onDaySelected(day) }
                    )
            ) {
                Box(modifier = Modifier.size(38.dp, 44.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = if (isPressed && !isAssigned) rimDepth else 0.dp)
                            .background(
                                if (isAssigned) AwanTheme.colors.disabledSurface.copy(alpha = 0.5f) else rimColor,
                                RoundedCornerShape(12.dp)
                            )
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = if ((isPressed || isAssigned) && !isSelected) 0.dp else rimDepth)
                            .background(
                                color = when {
                                    isAssigned -> AwanTheme.colors.disabledSurface
                                    templateColor != null -> templateColor.copy(alpha = if (isSelected) 0.1f else 0.06f).compositeOver(surface)
                                    isSelected -> AwanTheme.colors.sky.copy(alpha = 0.08f).compositeOver(surface)
                                    else -> surface
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = when {
                                    isAssigned -> AwanTheme.colors.line.copy(alpha = 0.5f)
                                    isSelected && templateColor != null -> templateColor
                                    isSelected -> AwanTheme.colors.sky
                                    isToday -> AwanTheme.colors.sky
                                    templateColor != null -> templateColor.copy(alpha = 0.3f).compositeOver(surface)
                                    else -> AwanTheme.colors.line
                                },
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        AwanText(
                            text = abbreviation.take(1),
                            style = AwanTheme.styles.bodyText.copy(
                                textStyle = AwanTheme.styles.bodyText.textStyle.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = when {
                                    isAssigned -> AwanTheme.colors.disabledContent
                                    templateColor != null -> templateColor
                                    isSelected -> AwanTheme.colors.sky
                                    else -> AwanTheme.colors.textPrimary
                                }
                            )
                        )
                    }
                }
                if (isToday && !isSelected) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (isAssigned) AwanTheme.colors.disabledContent else AwanTheme.colors.sky)
                    )
                } else {
                    Spacer(modifier = Modifier.size(4.dp))
                }

                AwanText(
                    text = abbreviation,
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp),
                        color = when {
                            isAssigned -> AwanTheme.colors.disabledContent
                            templateColor != null -> templateColor
                            isSelected -> AwanTheme.colors.sky
                            else -> AwanTheme.colors.textSecondary
                        }
                    )
                )
            }
        }
    }
}
