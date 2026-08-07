package com.awan.feature.home.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.SessionTaskDetail
import com.awan.feature.home.impl.R

@Composable
internal fun UnifiedSessionTaskContent(
    detail: SessionTaskDetail,
    onDismiss: () -> Unit,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isCompleted = detail.session.status.uppercase() == "COMPLETED" ||
        detail.task.status.uppercase() == "COMPLETED"
    val isLocked = detail.session.locked

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        // Top Header Row with Category Badge & Close Icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val defaultCategory = stringResource(R.string.home_task_default_category)
            val categoryName = (detail.task.categoryName ?: defaultCategory).uppercase()
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.12f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                AwanText(
                    text = categoryName,
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AwanTheme.colors.sky,
                        letterSpacing = 0.8.sp,
                    ),
                )
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp),
            ) {
                AwanText(
                    text = "✕",
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 16.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Task Title & Description Header
        AwanText(
            text = detail.task.title,
            style = AwanTheme.typography.heading.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textPrimary,
                lineHeight = 26.sp,
            ),
        )

        detail.task.description?.takeIf { it.isNotBlank() }?.let { desc ->
            Spacer(modifier = Modifier.height(6.dp))
            AwanText(
                text = desc,
                style = AwanTheme.typography.body.copy(
                    fontSize = 14.sp,
                    color = AwanTheme.colors.textSecondary,
                    lineHeight = 20.sp,
                ),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Highlighted Session Duration & Time Card
            val dateStr = formatIsoDate(detail.session.start)
            val startTime = formatIsoTime(detail.session.start)
            val endTime = formatIsoTime(detail.session.end)
            val durationMins = calculateDurationMinutes(detail.session.start, detail.session.end)
                ?: detail.task.estimatedDuration ?: 30

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(AwanTheme.colors.surface)
                    .border(
                        width = 1.dp,
                        color = AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        AwanText(
                            text = "$startTime - $endTime",
                            style = AwanTheme.typography.heading.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AwanTheme.colors.textPrimary,
                            ),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        AwanText(
                            text = dateStr,
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                color = AwanTheme.colors.textSecondary,
                            ),
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AwanTheme.colors.sky.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        AwanText(
                            text = "⏱ " + stringResource(R.string.home_task_duration_value, durationMins),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AwanTheme.colors.sky,
                            ),
                        )
                    }
                }
            }

            // Quick Info Chips Row (Points, Mandatory, Splitting)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (detail.task.estimatedPoints > 0) {
                    InfoChip(
                        icon = "🏆",
                        label = stringResource(R.string.home_task_points_value, detail.task.estimatedPoints),
                        modifier = Modifier.weight(1f),
                    )
                }

                InfoChip(
                    icon = if (detail.task.mandatory) "❗" else "💡",
                    label = if (detail.task.mandatory) stringResource(R.string.home_task_mandatory)
                    else stringResource(R.string.home_task_optional),
                    modifier = Modifier.weight(1f),
                )

                InfoChip(
                    icon = "🔀",
                    label = if (detail.task.allowTaskSplitting) stringResource(R.string.home_task_splitting_allowed)
                    else stringResource(R.string.home_task_splitting_not_allowed),
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.4f))

            // Metadata Info Rows
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val statusString = when (detail.session.status.uppercase()) {
                    "COMPLETED" -> stringResource(R.string.home_status_completed)
                    "IN_PROGRESS" -> stringResource(R.string.home_status_in_progress)
                    "SKIPPED" -> stringResource(R.string.home_status_skipped)
                    else -> stringResource(R.string.home_status_scheduled)
                }

                DetailRow(
                    icon = "📊",
                    title = stringResource(R.string.home_session_status_label),
                    value = statusString,
                    isBadge = true,
                    isSuccessBadge = isCompleted,
                )

                DetailRow(
                    icon = if (isLocked) "🔒" else "🔓",
                    title = stringResource(R.string.home_session_locked_label),
                    value = if (isLocked) stringResource(R.string.home_session_locked_yes)
                    else stringResource(R.string.home_session_locked_no),
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Action Buttons Bar (Mark Done & Lock/Unlock side by side)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Mark Done / Pending Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isCompleted) AwanTheme.colors.surface
                        else AwanTheme.colors.success
                    )
                    .border(
                        width = 1.dp,
                        color = if (isCompleted) AwanTheme.colors.line else Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(onClick = onToggleStatus)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AwanText(
                        text = if (isCompleted) "↩" else "✓",
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) AwanTheme.colors.textPrimary else Color.White,
                        ),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AwanText(
                        text = if (isCompleted) stringResource(R.string.home_action_mark_pending)
                        else stringResource(R.string.home_action_mark_done),
                        style = AwanTheme.typography.button.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCompleted) AwanTheme.colors.textPrimary else Color.White,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                }
            }

            // Lock / Unlock Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isLocked) AwanTheme.colors.sky.copy(alpha = 0.15f)
                        else AwanTheme.colors.surface
                    )
                    .border(
                        width = 1.dp,
                        color = if (isLocked) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable(onClick = onToggleLock)
                    .padding(vertical = 8.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    AwanText(
                        text = if (isLocked) "🔓" else "🔒",
                        style = AwanTheme.typography.body.copy(fontSize = 13.sp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    AwanText(
                        text = if (isLocked) stringResource(R.string.home_action_unlock)
                        else stringResource(R.string.home_action_lock),
                        style = AwanTheme.typography.button.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLocked) AwanTheme.colors.sky else AwanTheme.colors.textPrimary,
                            textAlign = TextAlign.Center,
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}
