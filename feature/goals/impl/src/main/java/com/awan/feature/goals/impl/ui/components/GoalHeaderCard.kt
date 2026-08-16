package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.app.core.model.GoalStatus
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Calendar
import com.composables.icons.lucide.Lucide

@Composable
internal fun GoalHeaderCard(goal: Goal, accentColor: Color) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                AwanText(
                    text = goal.title,
                    style = AwanTheme.typography.title.copy(
                        fontSize = 20.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
                
                // Status Badge
                val isAchieved = goal.progress >= 1f || goal.status == GoalStatus.ACHIEVED
                val badgeColor = if (isAchieved) colors.success else accentColor
                val statusText = when (goal.status) {
                    GoalStatus.ACTIVE -> stringResource(R.string.goals_status_active)
                    GoalStatus.ACHIEVED -> stringResource(R.string.goals_status_achieved)
                    GoalStatus.UNKNOWN -> goal.status.name
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    AwanText(
                        text = statusText,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    )
                }
            }
            
            val description = goal.description
            if (!description.isNullOrBlank()) {
                AwanText(
                    text = description,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 14.sp,
                        color = colors.textSecondary
                    )
                )
            }
            
            val targetDate = goal.targetDate
            if (!targetDate.isNullOrBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(spacing.xs)
                ) {
                    Icon(
                        imageVector = Lucide.Calendar,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                    AwanText(
                        text = targetDate,
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                    )
                }
            }
        }
    }
}
