package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.ChevronLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

@Composable
fun AwanHeaderBar(
    userName: String,
    streakCount: Int,
    pointsCount: Int = 0,
    mascotExpression: MascotExpression = MascotExpression.Greet,
    subtitleText: String = "Clear skies — 6 things floating today",
    greetingPrefix: String = "Good afternoon",
    selectedDateText: String = "Today · Wed, Jul 15",
    isCollapsed: Boolean = false,
    isToday: Boolean = true,
    totalSessionsCount: Int = 0,
    completedSessionsCount: Int = 0,
    completedHours: Double = 0.0,
    totalHours: Double = 0.0,
    scheduledHoursText: String = "",
    progressSegments: List<CategoryProgressSegment> = emptyList(),
    onPreviousDayClick: () -> Unit = {},
    onNextDayClick: () -> Unit = {},
    onDatePillClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val displayName = userName.ifBlank { stringResource(R.string.ds_friend) }

    val mascotWidth by animateDpAsState(
        targetValue = if (isCollapsed) 80.dp else 140.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mascotWidth",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                AnimatedVisibility(
                    visible = !isCollapsed,
                    enter = fadeIn(tween(220)) + expandVertically(tween(220)),
                    exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
                ) {
                    Column {
                        AwanText(
                            text = "$greetingPrefix,",
                            style = AwanTheme.styles.titleText,
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        AwanText(
                            text = displayName,
                            style = AwanTheme.styles.titleText,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AwanStreakBadge(streakCount = streakCount)

                    AwanPointsBadge(
                        pointsCount = pointsCount,
                        modifier = Modifier.rewardAnchor(
                            anchor = RewardAnchor.PointsBadge,
                            anchors = LocalRewardAnchors.current,
                        ),
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            AwanMascot(
                expression = mascotExpression,
                width = mascotWidth,
            )
        }

        AnimatedVisibility(
            visible = !isCollapsed,
            enter = fadeIn(tween(220)) + expandVertically(tween(220)),
            exit = fadeOut(tween(180)) + shrinkVertically(tween(180)),
        ) {
            AwanProgressSummaryCard(
                subtitle = stringResource(R.string.ds_tasks_count, completedSessionsCount, totalSessionsCount),
                completedHours = completedHours,
                totalHours = totalHours,
                segments = progressSegments,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavArrowButton2D(
                isNext = false,
                onClick = onPreviousDayClick,
            )

            DatePillButton2D(
                selectedDateText = selectedDateText,
                isToday = isToday,
                onClick = onDatePillClick,
                modifier = Modifier.weight(1f),
            )

            NavArrowButton2D(
                isNext = true,
                onClick = onNextDayClick,
            )
        }
    }
}

@Composable
private fun NavArrowButton2D(
    isNext: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val buttonShape = RoundedCornerShape(16.dp)
    val rimDepth = 3.5.dp
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) rimDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "navArrowPress",
    )

    val surfaceColor = AwanTheme.colors.surface
    val rimColor = AwanTheme.colors.line
    val iconColor = AwanTheme.colors.textPrimary

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 46.dp + rimDepth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    onClick()
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .offset(y = rimDepth)
                .size(46.dp)
                .clip(buttonShape)
                .background(rimColor),
        )

        Box(
            modifier = Modifier
                .offset(y = pressOffsetY)
                .size(46.dp)
                .shadow(
                    elevation = 2.dp,
                    shape = buttonShape,
                    spotColor = Color(0x1F000000),
                )
                .clip(buttonShape)
                .background(surfaceColor)
                .border(1.5.dp, rimColor, buttonShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (isNext) Lucide.ChevronRight else Lucide.ChevronLeft,
                contentDescription = if (isNext) "Next Day" else "Previous Day",
                tint = iconColor,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun DatePillButton2D(
    selectedDateText: String,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptics = LocalHapticFeedback.current

    val pillShape = RoundedCornerShape(18.dp)
    val rimDepth = 3.5.dp
    val pressOffsetY by animateDpAsState(
        targetValue = if (isPressed) rimDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "datePillPress",
    )

    val surfaceColor = if (isToday) AwanTheme.colors.sky else AwanTheme.colors.surface
    val rimColor = if (isToday) AwanTheme.colors.skyPressed else AwanTheme.colors.line
    val textColor = if (isToday) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary
    val iconColor = if (isToday) AwanTheme.colors.onSky else AwanTheme.colors.sky

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(46.dp + rimDepth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onClick()
                },
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .offset(y = rimDepth)
                .fillMaxWidth()
                .height(46.dp)
                .clip(pillShape)
                .background(rimColor),
        )

        Box(
            modifier = Modifier
                .offset(y = pressOffsetY)
                .fillMaxWidth()
                .height(46.dp)
                .shadow(
                    elevation = if (isToday) 4.dp else 2.dp,
                    shape = pillShape,
                    spotColor = if (isToday) AwanTheme.colors.sky.copy(alpha = 0.4f) else Color(0x1F000000),
                )
                .clip(pillShape)
                .background(surfaceColor)
                .border(
                    width = 1.5.dp,
                    color = if (isToday) Color.White.copy(alpha = 0.35f) else rimColor,
                    shape = pillShape,
                )
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = Lucide.Calendar,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(18.dp),
                )

                Spacer(modifier = Modifier.width(8.dp))

                AwanText(
                    text = selectedDateText,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                    ),
                )
            }
        }
    }
}
