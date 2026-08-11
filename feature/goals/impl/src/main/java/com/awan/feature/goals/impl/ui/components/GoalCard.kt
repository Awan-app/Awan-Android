package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanCloud
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide

@Composable
internal fun GoalCard(
    goal: Goal,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors

    AwanCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp) // Manual padding for cloud layering
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // ── Fantasy Cloud Background ──────────────────────────────────
            if (goal.progress > 0f) {
                AwanCloud(
                    size = 120.dp,
                    baseColor = accentColor.copy(alpha = 0.08f),
                    shadeColor = accentColor.copy(alpha = 0.04f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 30.dp, y = (-20).dp)
                        .alpha(0.6f)
                )
                
                if (goal.progress > 0.5f) {
                    AwanCloud(
                        size = 80.dp,
                        baseColor = accentColor.copy(alpha = 0.06f),
                        shadeColor = accentColor.copy(alpha = 0.03f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .offset(x = (-20).dp, y = 10.dp)
                            .alpha(0.5f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .zIndex(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Top Row (Icon, Title, Target) ──────────────────────────────
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Goal Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        AwanText(
                            text = goal.emoji,
                            style = AwanTheme.typography.heading.copy(fontSize = 24.sp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AwanText(
                                text = goal.title,
                                style = AwanTheme.typography.title.copy(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.ink
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (goal.isCompleted) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.success),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Lucide.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Lucide.ChevronRight,
                                    contentDescription = null,
                                    tint = colors.line,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                // ── Progress Section ───────────────────────────────────────────
                if (goal.totalTasks > 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AwanText(
                                text = stringResource(R.string.goals_progress_percentage, (goal.progress * 100).toInt()),
                                style = AwanTheme.typography.title.copy(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = accentColor
                                )
                            )
                            AwanText(
                                text = stringResource(R.string.goals_progress_format, goal.completedTasks, goal.totalTasks),
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 13.sp,
                                    color = colors.textSecondary
                                )
                            )
                        }

                        // Progress Bar
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                        ) {
                            val trackH = size.height
                            val radius = trackH / 2f
                            val progressWidth = size.width * goal.progress.coerceIn(0f, 1f)
                            
                            // Track
                            drawRoundRect(
                                color = colors.line,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                            )
                            
                            // Filled portion
                            if (progressWidth > 0f) {
                                drawRoundRect(
                                    color = accentColor,
                                    size = androidx.compose.ui.geometry.Size(progressWidth, trackH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                                )
                            }
                        }
                    }
                }

                // ── Bottom Chips ──────────────────────────────────────────
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (goal.completedTasks > 0) {
                        GoalStatusChip(
                            count = goal.completedTasks,
                            label = stringResource(R.string.goals_status_completed),
                            color = colors.success,
                            icon = Lucide.Check
                        )
                    }
                    
                    val activeTasks = goal.totalTasks - goal.completedTasks
                    if (activeTasks > 0) {
                        GoalStatusChip(
                            count = activeTasks,
                            label = stringResource(R.string.goals_status_active),
                            color = accentColor,
                            icon = null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalStatusChip(
    count: Int,
    label: String,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        
        AwanText(
            text = "$count $label",
            style = AwanTheme.typography.caption.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

@Composable
private fun TargetDateBadge(date: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(AwanTheme.colors.sky.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        AwanText(
            text = stringResource(R.string.goals_target_date, date),
            style = AwanTheme.typography.caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.sky
            )
        )
    }
}
