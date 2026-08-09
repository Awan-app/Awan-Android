package com.awan.feature.goals.impl.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Goal
import com.awan.app.core.model.TaskStatus
import com.awan.feature.goals.impl.R

/**
 * Card for a single goal.
 *
 * - Tapping the header row toggles the task list open/closed.
 * - The chevron arrow rotates 180° when expanded.
 * - Progress bar + "X of Y tasks done" are always visible.
 * - When expanded: all tasks shown — pending first, completed (strikethrough) last.
 */
@Composable
internal fun GoalCard(
    goal: Goal,
    accentColor: Color,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors
    val cardShape = RoundedCornerShape(20.dp)
    val hasTasks = goal.tasks.isNotEmpty()
    var expanded by rememberSaveable(goal.id) { mutableStateOf(false) }
    val chevronDeg by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "chevron",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 2.dp, shape = cardShape, spotColor = Color(0x18000000))
            .clip(cardShape)
            .background(colors.surface),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {

            // ── Tappable header row ───────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (hasTasks) Modifier.clickable { expanded = !expanded }
                        else Modifier,
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                // Emoji
                val emoji = goal.emoji
                if (emoji != null) {
                    AwanText(
                        text = emoji,
                        style = AwanTheme.typography.heading.copy(fontSize = 30.sp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }

                // Title (strikethrough on Completed tab)
                AwanText(
                    text = goal.title,
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.ink,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )

                // Chevron arrow
                val rotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
                Canvas(
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotation),
                ) {
                    val w = size.width
                    val h = size.height
                    val path = Path().apply {
                        moveTo(w * 0.2f, h * 0.35f)
                        lineTo(w * 0.5f, h * 0.65f)
                        lineTo(w * 0.8f, h * 0.35f)
                    }
                    drawPath(
                        path = path,
                        color = colors.textSecondary,
                        style = Stroke(
                            width = 2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }

            // ── Progress bar + stats (always visible) ────────────────────────
            if (goal.totalTasks > 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Canvas-drawn rounded progress bar with dot at progress end
                        Canvas(
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                        ) {
                            val trackH = size.height
                            val radius = trackH / 2f
                            val progressWidth = size.width * goal.progress.coerceIn(0f, 1f)
                            // Track
                            drawRoundRect(
                                color = colors.line,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                            )
                            // Filled portion + dot
                            if (progressWidth > 0f) {
                                drawRoundRect(
                                    color = accentColor,
                                    size = androidx.compose.ui.geometry.Size(progressWidth, trackH),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                                )
                                val dotR = trackH * 0.75f
                                val dotX = (progressWidth - dotR).coerceAtLeast(dotR)
                                drawCircle(
                                    color = accentColor,
                                    radius = dotR,
                                    center = Offset(dotX, trackH / 2f),
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        AwanText(
                            text = stringResource(R.string.goals_progress_percentage, (goal.progress * 100).toInt()),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor,
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    AwanText(
                        text = stringResource(R.string.goals_progress_format, goal.completedTasks, goal.totalTasks),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                        ),
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                exit = fadeOut(tween(150)) + shrinkVertically(tween(200)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
                ) {
                    HorizontalDivider(
                        color = colors.line,
                        thickness = 1.dp,
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    if (goal.tasks.isEmpty()) {
                        AwanText(
                            text = stringResource(R.string.goal_no_tasks),
                            style = AwanTheme.typography.body.copy(color = colors.textSecondary, fontSize = 14.sp)
                        )
                    } else {
                        // Pending tasks first, completed (strikethrough) last
                        val sortedTasks = goal.tasks.sortedBy { it.task.status == TaskStatus.COMPLETED }
                        sortedTasks.forEach { task ->
                            GoalTaskRow(
                                tws = task,
                                accentColor = accentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}
