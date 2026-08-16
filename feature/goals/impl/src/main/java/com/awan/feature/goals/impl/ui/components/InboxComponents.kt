package com.awan.feature.goals.impl.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.goals.impl.R
import com.awan.feature.goals.impl.presentation.InboxTaskUiModel
import com.awan.feature.goals.impl.presentation.InboxSessionUiModel
import com.composables.icons.lucide.ChevronDown
import com.composables.icons.lucide.Lucide

@Composable
internal fun InboxTaskCard(
    task: InboxTaskUiModel,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onTaskClick: () -> Unit,
) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onTaskClick,
        contentPadding = PaddingValues(0.dp) // Manual padding for clickable behavior
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = task.title,
                        style = AwanTheme.typography.heading.copy(
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    
                    if (!task.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.xxs))
                        AwanText(
                            text = task.description,
                            style = AwanTheme.typography.body.copy(
                                fontSize = 14.sp,
                                color = colors.textSecondary,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(spacing.sm))
                
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "expand_icon_rotation")
                val expandDescription = stringResource(
                    if (isExpanded) R.string.inbox_collapse_sessions_content_description
                    else R.string.inbox_expand_sessions_content_description
                )
                val hapticExpandToggle = rememberHapticClick(
                    onClick = onExpandToggle,
                    haptic = HapticFeedbackType.SegmentTick,
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable(onClick = hapticExpandToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Lucide.ChevronDown,
                        contentDescription = expandDescription,
                        tint = colors.textSecondary,
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.sm)
                ) {
                    HorizontalDivider(color = colors.line, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(spacing.sm))
                    
                    if (task.sessions.isEmpty()) {
                        AwanText(
                            text = stringResource(R.string.inbox_no_sessions),
                            style = AwanTheme.typography.body.copy(color = colors.textSecondary, fontSize = 14.sp)
                        )
                    } else {
                        task.sessions.forEachIndexed { index, session ->
                            InboxSessionRow(session = session)
                            if (index < task.sessions.lastIndex) {
                                Spacer(modifier = Modifier.height(spacing.xs))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun InboxSessionRow(session: InboxSessionUiModel) {
    val colors = AwanTheme.colors
    val spacing = AwanTheme.spacing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(if (session.isActiveNow) colors.sky else colors.line)
        )
        
        Spacer(modifier = Modifier.width(spacing.sm))
        
        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                text = session.dateLabel,
                style = AwanTheme.typography.button.copy(fontSize = 14.sp, color = colors.textPrimary)
            )
            AwanText(
                text = stringResource(R.string.inbox_session_time_range, session.startTime, session.endTime),
                style = AwanTheme.typography.caption.copy(fontSize = 12.sp, color = colors.textSecondary)
            )
        }
        
        Spacer(modifier = Modifier.width(spacing.xs))
        
        AwanChip(
            label = stringResource(session.statusLabelRes),
            active = false,
            tone = if (session.isMissed) AwanChipTone.Destructive else AwanChipTone.Neutral,
            onClick = null
        )
    }
}
