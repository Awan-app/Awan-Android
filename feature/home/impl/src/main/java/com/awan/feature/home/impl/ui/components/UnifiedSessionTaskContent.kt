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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.awan.app.core.model.SessionStatus
import com.awan.app.core.model.SessionTaskDetail
import com.awan.app.core.model.TaskStatus
import com.awan.feature.home.impl.R

@Composable
internal fun UnifiedSessionTaskContent(
    detail: SessionTaskDetail,
    onToggleStatus: () -> Unit,
    onToggleLock: () -> Unit,
    onEditClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val isCompleted = detail.session.status == SessionStatus.COMPLETED ||
        detail.task.status == TaskStatus.COMPLETED
    val isLocked = detail.session.locked

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (onEditClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AwanTheme.colors.sky.copy(alpha = 0.12f))
                            .clickable(onClick = onEditClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.home_action_edit),
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }

                if (onDeleteClick != null) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AwanTheme.colors.destructive.copy(alpha = 0.12f))
                            .clickable(onClick = onDeleteClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.home_action_delete),
                            tint = AwanTheme.colors.destructive,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
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
            val dateStr = formatDate(detail.session.start)
            val startTime = formatTime(detail.session.start)
            val endTime = formatTime(detail.session.end)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = AwanTheme.colors.sky,
                                modifier = Modifier.size(13.dp),
                            )
                            AwanText(
                                text = "${durationMins}m",
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AwanTheme.colors.sky,
                                ),
                            )
                        }
                    }
                }
            }

            // Task Related Sessions List (if multiple sessions exist for task)
            if (detail.relatedSessions.isNotEmpty()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AwanText(
                            text = stringResource(R.string.home_task_sessions_header),
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = AwanTheme.colors.textSecondary,
                                letterSpacing = 0.5.sp,
                            ),
                        )
                        val doneCount = detail.relatedSessions.count { it.status == SessionStatus.COMPLETED }
                        AwanText(
                            text = "$doneCount/${detail.relatedSessions.size} ${stringResource(R.string.home_status_completed)}",
                            style = AwanTheme.typography.caption.copy(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AwanTheme.colors.sky,
                            ),
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        detail.relatedSessions.forEachIndexed { index, s ->
                            val isCurrent = s.id == detail.session.id
                            val isSessionDone = s.status == SessionStatus.COMPLETED
                            val sStart = formatTime(s.start)
                            val sEnd = formatTime(s.end)

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isCurrent) AwanTheme.colors.sky.copy(alpha = 0.08f)
                                        else AwanTheme.colors.surface
                                    )
                                    .border(
                                        width = if (isCurrent) 1.5.dp else 1.dp,
                                        color = if (isCurrent) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(12.dp),
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (isSessionDone) Icons.Default.CheckCircle else Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (isSessionDone) AwanTheme.colors.success else AwanTheme.colors.sky,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        AwanText(
                                            text = stringResource(R.string.home_task_session_item, index + 1, sStart, sEnd),
                                            style = AwanTheme.typography.body.copy(
                                                fontSize = 12.5.sp,
                                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                                color = AwanTheme.colors.textPrimary,
                                            ),
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(
                                                if (isSessionDone) AwanTheme.colors.success.copy(alpha = 0.15f)
                                                else AwanTheme.colors.line.copy(alpha = 0.3f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 3.dp),
                                    ) {
                                        AwanText(
                                            text = if (isSessionDone) stringResource(R.string.home_status_completed)
                                            else stringResource(R.string.home_status_scheduled),
                                            style = AwanTheme.typography.caption.copy(
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSessionDone) AwanTheme.colors.success else AwanTheme.colors.textSecondary,
                                            ),
                                        )
                                    }
                                }
                            }
                        }
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
                        icon = Icons.Default.Star,
                        label = stringResource(R.string.home_task_points_value, detail.task.estimatedPoints),
                        modifier = Modifier.weight(1f),
                    )
                }

                InfoChip(
                    icon = if (detail.task.mandatory) Icons.Default.PushPin else Icons.Default.Lightbulb,
                    label = if (detail.task.mandatory) stringResource(R.string.home_task_mandatory)
                    else stringResource(R.string.home_task_optional),
                    modifier = Modifier.weight(1f),
                )

                InfoChip(
                    icon = Icons.AutoMirrored.Filled.CallSplit,
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
                val statusString = when (detail.session.status) {
                    SessionStatus.COMPLETED -> stringResource(R.string.home_status_completed)
                    SessionStatus.IN_PROGRESS -> stringResource(R.string.home_status_in_progress)
                    else -> stringResource(R.string.home_status_scheduled)
                }

                DetailRow(
                    label = stringResource(R.string.home_session_status_label),
                    value = statusString,
                )

                DetailRow(
                    label = stringResource(R.string.home_session_locked_label),
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
                    Icon(
                        imageVector = if (isCompleted) Icons.AutoMirrored.Filled.Undo else Icons.Default.Check,
                        contentDescription = null,
                        tint = if (isCompleted) AwanTheme.colors.textPrimary else Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
                    Icon(
                        imageVector = if (isLocked) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = null,
                        tint = if (isLocked) AwanTheme.colors.sky else AwanTheme.colors.textPrimary,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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
