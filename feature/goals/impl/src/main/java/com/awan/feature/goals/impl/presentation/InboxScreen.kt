package com.awan.feature.goals.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.MascotExpression
import com.awan.feature.goals.impl.R

@Composable
fun InboxScreen(
    state: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AwanTheme.colors

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Search Bar
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                AwanTextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(InboxAction.SearchQueryChanged(it)) },
                    placeholder = stringResource(R.string.inbox_search_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Status Filters
                InboxTaskDisplayStatus.entries.forEach { status ->
                    val isSelected = status in state.activeStatusFilters
                    val labelRes = when (status) {
                        InboxTaskDisplayStatus.Drafted -> R.string.inbox_status_drafted
                        InboxTaskDisplayStatus.Active -> R.string.inbox_status_active
                        InboxTaskDisplayStatus.Completed -> R.string.inbox_status_completed
                        InboxTaskDisplayStatus.Cancelled -> R.string.inbox_status_cancelled
                        InboxTaskDisplayStatus.Missed -> R.string.inbox_status_missed
                    }
                    AwanChip(
                        label = stringResource(labelRes),
                        active = isSelected,
                        tone = if (isSelected) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(InboxAction.StatusFilterToggled(status)) }
                    )
                }

                // Session Filters
                InboxSessionFilter.entries.forEach { filter ->
                    val isSelected = filter in state.activeSessionFilters
                    val labelRes = when (filter) {
                        InboxSessionFilter.ActiveNow -> R.string.inbox_filter_active_now
                        InboxSessionFilter.Missed -> R.string.inbox_filter_missed
                    }
                    AwanChip(
                        label = stringResource(labelRes),
                        active = isSelected,
                        tone = if (isSelected) AwanChipTone.Sky else AwanChipTone.Neutral,
                        onClick = { onAction(InboxAction.SessionFilterToggled(filter)) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                when {
                    state.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(
                                color = colors.sky,
                                strokeWidth = 3.dp,
                            )
                        }
                    }

                    state.isError -> {
                        InboxErrorState(onRetry = { onAction(InboxAction.RetryClicked) })
                    }

                    state.visibleTasks.isEmpty() -> {
                        InboxEmptyState()
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            items(state.visibleTasks, key = { it.id }) { task ->
                                InboxTaskCard(
                                    task = task,
                                    isExpanded = state.expandedTaskId == task.id,
                                    onExpandToggle = { onAction(InboxAction.TaskExpandToggled(task.id)) }
                                )
                            }
                            item { Spacer(modifier = Modifier.height(24.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxTaskCard(
    task: InboxTaskUiModel,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
) {
    val colors = AwanTheme.colors
    val cardShape = RoundedCornerShape(16.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(1.dp, colors.line, cardShape),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(16.dp),
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
                        Spacer(modifier = Modifier.height(4.dp))
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

                Spacer(modifier = Modifier.width(12.dp))
                
                val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "expand_icon_rotation")
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Assuming we have some dropdown icon, using a generic placeholder or unicode if no icon
                    // For now, let's use a simple text chevron or a loaded resource if exists
                    AwanText(
                        text = "▼",
                        style = AwanTheme.typography.button.copy(color = colors.textSecondary),
                        modifier = Modifier.rotate(rotation)
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    HorizontalDivider(color = colors.line, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    if (task.sessions.isEmpty()) {
                        AwanText(
                            text = stringResource(R.string.inbox_no_sessions),
                            style = AwanTheme.typography.body.copy(color = colors.textSecondary, fontSize = 14.sp)
                        )
                    } else {
                        task.sessions.forEach { session ->
                            InboxSessionRow(session = session)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InboxSessionRow(session: InboxSessionUiModel) {
    val colors = AwanTheme.colors

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Status indicator line/dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (session.isActiveNow) colors.sky else colors.line)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                text = session.dateLabel,
                style = AwanTheme.typography.button.copy(fontSize = 14.sp, color = colors.textPrimary)
            )
            AwanText(
                text = "${session.startTime} - ${session.endTime}",
                style = AwanTheme.typography.caption.copy(fontSize = 12.sp, color = colors.textSecondary)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        AwanChip(
            label = stringResource(session.statusLabelRes),
            active = false,
            tone = if (session.isMissed) AwanChipTone.Destructive else AwanChipTone.Neutral,
            onClick = null
        )
    }
}

@Composable
private fun InboxEmptyState() {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 90.dp)
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.inbox_empty_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 18.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(6.dp))
        AwanText(
            text = stringResource(R.string.inbox_empty_subtitle),
            style = AwanTheme.typography.body.copy(
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
private fun InboxErrorState(onRetry: () -> Unit) {
    val colors = AwanTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(text = "☁️", style = AwanTheme.typography.display.copy(fontSize = 48.sp))
        Spacer(modifier = Modifier.height(16.dp))
        AwanText(
            text = stringResource(R.string.inbox_error_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 16.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = Modifier.height(16.dp))
        AwanButton(
            onClick = onRetry,
            variant = AwanButtonVariant.Primary,
        ) {
            AwanText(
                text = stringResource(R.string.inbox_error_retry),
                style = AwanTheme.typography.button,
            )
        }
    }
}
