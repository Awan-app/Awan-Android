package com.awan.app.core.designsystem

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

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
        targetValue = if (isCollapsed) 96.dp else 160.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "mascotWidth",
    )

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
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
            visible = isCollapsed,
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

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavArrowButton3D(
                arrowText = "‹",
                onClick = onPreviousDayClick,
            )

            val pillShape = RoundedCornerShape(18.dp)
            val surfaceColor = AwanTheme.colors.surface
            val lineColor = AwanTheme.colors.line
            val textPrimaryColor = AwanTheme.colors.textPrimary
            val shadowColor = AwanTheme.colors.line
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
                    .shadow(
                        elevation = 4.dp,
                        shape = pillShape,
                        spotColor = shadowColor,
                    )
                    .clip(pillShape)
                    .background(surfaceColor)
                    .border(1.5.dp, lineColor, pillShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDatePillClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                AwanText(
                    text = selectedDateText,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 15.sp,
                        color = textPrimaryColor,
                    ),
                )
            }

            NavArrowButton3D(
                arrowText = "›",
                onClick = onNextDayClick,
            )
        }
    }
}

@Composable
private fun NavArrowButton3D(
    arrowText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val buttonShape = RoundedCornerShape(16.dp)
    val surfaceColor = AwanTheme.colors.surface
    val lineColor = AwanTheme.colors.line
    val textPrimaryColor = AwanTheme.colors.textPrimary

    Box(
        modifier = modifier
            .size(46.dp)
            .shadow(
                elevation = 4.dp,
                shape = buttonShape,
                spotColor = lineColor,
            )
            .clip(buttonShape)
            .background(surfaceColor)
            .border(1.5.dp, lineColor, buttonShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        AwanText(
            text = arrowText,
            style = AwanTheme.typography.title.copy(
                fontSize = 20.sp,
                color = textPrimaryColor,
            ),
        )
    }
}
