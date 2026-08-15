package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.GoalsAction
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Target

@Composable
internal fun GoalCard(
    goal: Goal,
    onClick: () -> Unit,
    onAction: (GoalsAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    val accentColor = goalAccentColor(goal.id.hashCode())
    val isAchieved = goal.progress >= 1f || goal.status == com.awan.app.core.model.GoalStatus.ACHIEVED

    AwanCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(spacing.md),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                // Goal Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.Target,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AwanText(
                            text = goal.title,
                            style = AwanTheme.typography.title.copy(
                                fontSize = 17.sp,
                                color = if (isAchieved) colors.textSecondary else colors.textPrimary,
                                textDecoration = if (isAchieved) TextDecoration.LineThrough else null
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        
                        if (isAchieved) {
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.success.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                AwanText(
                                    text = stringResource(R.string.goals_status_achieved),
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.success
                                    )
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(spacing.xs))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(accentColor.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                AwanText(
                                    text = stringResource(R.string.goals_status_active_badge),
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = accentColor
                                    )
                                )
                            }
                        }
                    }
                }

                AwanText(
                    text = stringResource(R.string.goals_progress_format, goal.completedTasks, goal.totalTasks),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor,
                    ),
                )
            }

            AwanText(
                text = goal.description ?: "",
                style = AwanTheme.styles.bodySecondaryText.textStyle.copy(
                    fontSize = 14.sp,
                    color = colors.textSecondary
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(colors.line),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(goal.progress.coerceIn(0f, 1f))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(accentColor),
                )
            }

            // Created At
            if (goal.createdAt.isNotEmpty()) {
                val date = goal.createdAt.take(10) // Simple YYYY-MM-DD for now
                AwanText(
                    text = stringResource(R.string.goals_created_at_format, date),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 10.sp,
                        color = colors.meta,
                        fontWeight = FontWeight.Medium
                    ),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}
