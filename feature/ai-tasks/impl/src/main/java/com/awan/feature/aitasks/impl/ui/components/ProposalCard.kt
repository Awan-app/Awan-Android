package com.awan.feature.aitasks.impl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBadge
import com.awan.app.core.designsystem.AwanBadgeTone
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanChipDotSize
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanDropdownMenu
import com.awan.app.core.designsystem.AwanDropdownMenuItem
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.Category
import com.awan.app.core.model.ProposedSession
import com.awan.app.core.model.TaskDraft
import com.awan.feature.aitasks.impl.R
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.X
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private val DurationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)

@Composable
fun ProposalCard(
    draft: TaskDraft,
    sessions: List<ProposedSession>,
    reason: String?,
    isExpanded: Boolean,
    categories: List<Category>,
    onRemoved: () -> Unit,
    onToggleExpanded: () -> Unit,
    onTitleChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onDurationPicked: (Int) -> Unit,
    onCategoryPicked: (String?) -> Unit,
    onMandatoryToggled: () -> Unit,
    onSessionTapped: (Int) -> Unit,
    onSessionRemoved: (Int) -> Unit,
    onSessionAdded: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    // The card itself opens the details; the X is the only thing that drops the task, so the two
    // gestures never fight over the same tap.
    AwanCard(
        modifier = modifier.fillMaxWidth(),
        onClick = {
            haptics.performHapticFeedback(
                if (isExpanded) HapticFeedbackType.SegmentTick else HapticFeedbackType.ContextClick,
            )
            onToggleExpanded()
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
                if (isExpanded) {
                    AwanTextField(
                        value = draft.title,
                        onValueChange = onTitleChanged,
                        placeholder = stringResource(R.string.ai_tasks_title_placeholder),
                        contentDescriptionText = stringResource(R.string.ai_tasks_title_placeholder),
                        textStyle = AwanTheme.styles.headingText,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    AwanText(
                        text = draft.title.ifBlank { stringResource(R.string.ai_tasks_title_placeholder) },
                        style = AwanTheme.styles.headingText,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (draft.estimatedPoints > 0) {
                    AwanBadge(
                        text = stringResource(R.string.ai_tasks_points_badge, draft.estimatedPoints),
                        tone = AwanBadgeTone.Sky,
                    )
                }
                AwanIconButton(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Reject)
                        onRemoved()
                    },
                    contentDescription = stringResource(R.string.ai_tasks_remove_content_description),
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(imageVector = Lucide.X, contentDescription = null, tint = AwanTheme.colors.textSecondary)
                }
            }

            if (isExpanded) {
                AwanTextField(
                    value = draft.description.orEmpty(),
                    onValueChange = onDescriptionChanged,
                    placeholder = stringResource(R.string.ai_tasks_description_placeholder),
                    contentDescriptionText = stringResource(R.string.ai_tasks_description_placeholder),
                    textStyle = AwanTheme.styles.bodySecondaryText,
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                )

                AttributeChips(
                    draft = draft,
                    categories = categories,
                    onDurationPicked = onDurationPicked,
                    onCategoryPicked = onCategoryPicked,
                    onMandatoryToggled = onMandatoryToggled,
                )

                reason?.let {
                    AwanText(
                        text = stringResource(R.string.ai_tasks_reason_prefix, it),
                        style = AwanTheme.styles.metaText,
                    )
                }
            } else {
                SummaryRow(draft = draft, categories = categories)
            }

            SessionsList(
                sessions = sessions,
                onSessionTapped = onSessionTapped,
                onSessionRemoved = onSessionRemoved,
                onSessionAdded = onSessionAdded,
            )
        }
    }
}

/** Read-only stand-in for [AttributeChips] while the card is collapsed — nothing here opens a menu. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryRow(draft: TaskDraft, categories: List<Category>) {
    val resolvedCategory = categories.firstOrNull { it.id == draft.categoryId }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        AwanBadge(
            text = draft.durationMinutes?.let { durationLabel(it) } ?: stringResource(R.string.ai_tasks_chip_no_duration),
            tone = AwanBadgeTone.Violet,
        )
        AwanBadge(
            text = resolvedCategory?.name ?: stringResource(R.string.ai_tasks_chip_no_category),
            tone = AwanBadgeTone.Neutral,
        )
        AwanBadge(
            text = stringResource(if (draft.mandatory) R.string.ai_tasks_chip_mandatory else R.string.ai_tasks_chip_optional),
            tone = AwanBadgeTone.Tangerine,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributeChips(
    draft: TaskDraft,
    categories: List<Category>,
    onDurationPicked: (Int) -> Unit,
    onCategoryPicked: (String?) -> Unit,
    onMandatoryToggled: () -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        var durationMenuOpen by remember { mutableStateOf(false) }
        Box {
            AwanChip(
                label = draft.durationMinutes?.let { durationLabel(it) }
                    ?: stringResource(R.string.ai_tasks_chip_no_duration),
                tone = AwanChipTone.Violet,
                active = draft.durationMinutes != null,
                onClick = { durationMenuOpen = true },
            )
            AwanDropdownMenu(expanded = durationMenuOpen, onDismissRequest = { durationMenuOpen = false }) {
                DurationPresets.forEach { minutes ->
                    val active = minutes == draft.durationMinutes
                    AwanDropdownMenuItem(
                        label = durationLabel(minutes),
                        onClick = { onDurationPicked(minutes); durationMenuOpen = false },
                        selected = active,
                        leading = { AwanChipDot(tone = AwanChipTone.Violet, active = active) },
                    )
                }
            }
        }

        var categoryMenuOpen by remember { mutableStateOf(false) }
        val resolvedCategory = categories.firstOrNull { it.id == draft.categoryId }
        Box {
            AwanChip(
                label = resolvedCategory?.name ?: stringResource(R.string.ai_tasks_chip_no_category),
                tone = AwanChipTone.Lavender,
                active = resolvedCategory != null,
                onClick = { categoryMenuOpen = true },
            )
            AwanDropdownMenu(expanded = categoryMenuOpen, onDismissRequest = { categoryMenuOpen = false }) {
                if (categories.isEmpty()) {
                    AwanDropdownMenuItem(
                        label = stringResource(R.string.ai_tasks_category_menu_empty),
                        onClick = {},
                        enabled = false,
                    )
                } else {
                    // Awan's category guess is a guess; clearing it has to be reachable.
                    AwanDropdownMenuItem(
                        label = stringResource(R.string.ai_tasks_chip_no_category),
                        onClick = { onCategoryPicked(null); categoryMenuOpen = false },
                        selected = draft.categoryId == null,
                        leading = { AwanChipDot(tone = AwanChipTone.Lavender, active = false) },
                    )
                }
                categories.forEach { category ->
                    val active = category.id == draft.categoryId
                    AwanDropdownMenuItem(
                        label = category.name,
                        onClick = { onCategoryPicked(category.id); categoryMenuOpen = false },
                        selected = active,
                        leading = { AwanChipDot(tone = AwanChipTone.Lavender, active = active) },
                    )
                }
            }
        }

        AwanChip(
            label = stringResource(
                if (draft.mandatory) R.string.ai_tasks_chip_mandatory else R.string.ai_tasks_chip_optional,
            ),
            tone = AwanChipTone.Tangerine,
            active = draft.mandatory,
            onClick = onMandatoryToggled,
        )
    }
}

@Composable
private fun SessionsList(
    sessions: List<ProposedSession>,
    onSessionTapped: (Int) -> Unit,
    onSessionRemoved: (Int) -> Unit,
    onSessionAdded: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
        if (sessions.isEmpty()) {
            AwanText(stringResource(R.string.ai_tasks_no_sessions), style = AwanTheme.styles.metaText)
        }
        sessions.forEachIndexed { index, session ->
            SessionRow(
                session = session,
                onClick = { onSessionTapped(index) },
                onRemove = { onSessionRemoved(index) },
            )
        }
        AwanButton(onClick = onSessionAdded, variant = AwanButtonVariant.Quiet, icon = { Icon(Lucide.Plus, null) }) {
            AwanText(stringResource(R.string.ai_tasks_session_add))
        }
    }
}

/**
 * Awan's own pick wears the sparkle in place of the chip's dot and a violet tone — a badge beside the
 * chip cost more width than the time itself, which wrapped the one thing the row exists to show.
 */
@Composable
private fun SessionRow(session: ProposedSession, onClick: () -> Unit, onRemove: () -> Unit) {
    val colors = AwanTheme.colors
    val tone = if (session.isAiSuggested) AwanChipTone.Violet else AwanChipTone.Sky
    // The sparkle stands in for the chip's dot, so it has to carry the dot's colour for that tone.
    val toneColor = if (session.isAiSuggested) colors.zoneViolet else colors.zoneSky
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AwanChip(
            label = sessionRangeLabel(session.start, session.end),
            tone = tone,
            onClick = onClick,
            leading = {
                if (session.isAiSuggested) {
                    Icon(
                        imageVector = Lucide.Sparkles,
                        contentDescription = stringResource(R.string.ai_tasks_session_suggested),
                        tint = toneColor,
                        modifier = Modifier.size(AwanChipDotSize + 6.dp),
                    )
                } else {
                    AwanChipDot(tone = tone)
                }
            },
            modifier = Modifier.weight(1f, fill = false),
        )
        AwanIconButton(
            onClick = onRemove,
            contentDescription = stringResource(R.string.ai_tasks_session_remove_content_description),
            modifier = Modifier.size(32.dp),
        ) {
            Icon(imageVector = Lucide.X, contentDescription = null, tint = colors.textSecondary)
        }
    }
}

@Composable
private fun sessionRangeLabel(start: LocalDateTime, end: LocalDateTime): String {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)
    return stringResource(
        R.string.ai_tasks_session_range,
        start.toLocalDate().format(dateFormatter),
        start.toLocalTime().format(timeFormatter),
        end.toLocalTime().format(timeFormatter),
    )
}
