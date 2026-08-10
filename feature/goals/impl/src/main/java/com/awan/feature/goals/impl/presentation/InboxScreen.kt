package com.awan.feature.goals.impl.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.style.styleable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
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
import com.awan.feature.goals.impl.ui.components.GoalsMascotHeader

@Composable
fun InboxScreen(
    state: InboxUiState,
    onAction: (InboxAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = AwanTheme.spacing

    Column(
        modifier = modifier
            .fillMaxSize()
            .styleable(null, AwanTheme.styles.screen)
            .statusBarsPadding()
    ) {
        // ── Header ────────────────────────────────────────────────────────
        GoalsMascotHeader()

        Spacer(modifier = Modifier.height(spacing.md))

        // ── Search Bar ────────────────────────────────────────────────────
        Box(modifier = Modifier.padding(horizontal = spacing.xl)) {
            AwanTextField(
                value = state.searchQuery,
                onValueChange = { onAction(InboxAction.SearchQueryChanged(it)) },
                placeholder = stringResource(R.string.inbox_search_placeholder),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(spacing.sm))

        // ── Filters ───────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = spacing.xl),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs)
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

        Spacer(modifier = Modifier.height(spacing.md))

        // ── Content area ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(
                        color = AwanTheme.colors.sky,
                        strokeWidth = 3.dp,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                state.isError -> {
                    InboxErrorState(onRetry = { onAction(InboxAction.RetryClicked) })
                }

                state.visibleTasks.isEmpty() -> {
                    InboxEmptyState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = spacing.xl,
                            vertical = spacing.md
                        ),
                        verticalArrangement = Arrangement.spacedBy(spacing.sm),
                    ) {
                        items(state.visibleTasks, key = { it.id }) { task ->
                            InboxTaskCard(
                                task = task,
                                isExpanded = state.expandedTaskId == task.id,
                                onExpandToggle = { onAction(InboxAction.TaskExpandToggled(task.id)) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(112.dp)) }
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
    val spacing = AwanTheme.spacing
    val cardShape = AwanTheme.shapes.card

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(colors.surface)
            .border(2.dp, colors.line, cardShape),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onExpandToggle)
                    .padding(spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = task.title,
                        style = AwanTheme.styles.headingText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    if (!task.description.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(spacing.xxs))
                        AwanText(
                            text = task.description,
                            style = AwanTheme.styles.bodySecondaryText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Box(
                    modifier = Modifier.size(spacing.xl),
                    contentAlignment = Alignment.Center
                ) {
                    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f, label = "expand_icon_rotation")
                    Canvas(modifier = Modifier.size(spacing.md).rotate(rotation)) {
                        val w = size.width
                        val h = size.height
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.2f, h * 0.35f)
                            lineTo(w * 0.5f, h * 0.65f)
                            lineTo(w * 0.8f, h * 0.35f)
                        }
                        drawPath(
                            path = path,
                            color = colors.textSecondary,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 2.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round,
                            ),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = spacing.md, vertical = spacing.xs)
                ) {
                    HorizontalDivider(color = colors.line, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(spacing.sm))

                    if (task.sessions.isEmpty()) {
                        AwanText(
                            text = stringResource(R.string.inbox_no_sessions),
                            style = AwanTheme.styles.bodySecondaryText
                        )
                    } else {
                        task.sessions.forEach { session ->
                            InboxSessionRow(session = session)
                            Spacer(modifier = Modifier.height(spacing.xs))
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
    val spacing = AwanTheme.spacing

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Status indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(if (session.isActiveNow) colors.sky else colors.line)
        )

        Spacer(modifier = Modifier.width(spacing.sm))

        Column(modifier = Modifier.weight(1f)) {
            AwanText(
                text = session.dateLabel,
                style = AwanTheme.styles.bodyText
            )
            AwanText(
                text = "${session.startTime} - ${session.endTime}",
                style = AwanTheme.styles.captionText
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

@Composable
private fun InboxEmptyState() {
    val spacing = AwanTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 90.dp)
        Spacer(modifier = Modifier.height(spacing.md))
        AwanText(
            text = stringResource(R.string.inbox_empty_title),
            style = AwanTheme.styles.titleText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.xxs))
        AwanText(
            text = stringResource(R.string.inbox_empty_subtitle),
            style = AwanTheme.styles.bodySecondaryText,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun InboxErrorState(onRetry: () -> Unit) {
    val spacing = AwanTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanText(
            text = "☁️",
            style = AwanTheme.styles.displayText,
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(spacing.md))
        AwanText(
            text = stringResource(R.string.inbox_error_title),
            style = AwanTheme.styles.headingText,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(spacing.md))
        AwanButton(
            onClick = onRetry,
            variant = AwanButtonVariant.Primary,
        ) {
            AwanText(
                text = stringResource(R.string.inbox_error_retry),
                style = AwanTheme.styles.buttonLabel,
            )
        }
    }
}

