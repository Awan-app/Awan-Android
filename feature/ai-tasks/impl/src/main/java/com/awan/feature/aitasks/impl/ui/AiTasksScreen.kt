package com.awan.feature.aitasks.impl.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanConfirmDialog
import com.awan.app.core.designsystem.AwanDatePickerDialog
import com.awan.app.core.designsystem.AwanDropdownMenu
import com.awan.app.core.designsystem.AwanDropdownMenuItem
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanMascot
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanTimePickerDialog
import com.awan.app.core.designsystem.AwanUriImage
import com.awan.app.core.designsystem.CascadeItem
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.app.core.designsystem.reducedMotion
import com.awan.app.core.model.Goal
import com.awan.feature.aitasks.impl.R
import com.awan.feature.aitasks.impl.presentation.AiTasksAction
import com.awan.feature.aitasks.impl.presentation.AiTasksEvent
import com.awan.feature.aitasks.impl.presentation.AiTasksState
import com.awan.feature.aitasks.impl.presentation.AiTasksViewModel
import com.awan.feature.aitasks.impl.presentation.PickerStep
import com.awan.feature.aitasks.impl.ui.components.MinutesPerHour
import com.awan.feature.aitasks.impl.ui.components.ProposalCard
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import kotlinx.coroutines.delay

@Composable
fun AiTasksRouteScreen(
    text: String = "",
    note: String? = null,
    imageUri: String? = null,
    goalId: String? = null,
    onBack: () -> Unit,
    onTasksCreated: (Int) -> Unit = { onBack() },
    viewModel: AiTasksViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(
            AiTasksAction.Load(
                text = text,
                note = note,
                imageUri = imageUri,
                goalId = goalId,
            )
        )
    }

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AiTasksEvent.TasksCreated -> onTasksCreated(event.count)
            AiTasksEvent.Dismissed -> onBack()
        }
    }

    BackHandler { viewModel.onAction(AiTasksAction.BackRequested) }

    AiTasksScreen(
        state = state,
        onAction = viewModel::onAction,
        onBack = { viewModel.onAction(AiTasksAction.BackRequested) },
    )
}

@Composable
private fun AiTasksScreen(
    state: AiTasksState,
    onAction: (AiTasksAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background),
    ) {
        Header(onCancel = onBack)

        Box(modifier = Modifier.weight(1f)) {
            val reduced = reducedMotion()
            Crossfade(
                targetState = screenPhase(state),
                animationSpec = if (reduced) snap() else tween(AwanTheme.motion.emphasizedMillis),
                label = "aiTasksBody",
            ) { phase ->
                when (phase) {
                    ScreenPhase.Loading -> LoadingBody()
                    // messageRes is captured on the phase itself, not read live off `state` — the
                    // outgoing composition must not force-unwrap a field Retry has since nulled out.
                    is ScreenPhase.Error -> ErrorBody(
                        messageRes = phase.messageRes,
                        onRetry = { onAction(AiTasksAction.Retry) },
                    )

                    ScreenPhase.Empty -> EmptyBody(onRetry = { onAction(AiTasksAction.Retry) })
                    ScreenPhase.Content -> ProposalsBody(state = state, onAction = onAction)
                }
            }
        }

        // Keyed off the *original* list, not the current one: removing the last card must leave the
        // bar standing, or the undo and the reset link go with it.
        AnimatedVisibility(
            visible = state.originalProposals.isNotEmpty(),
            enter = slideInVertically(settleSpec()) { it } + fadeIn(),
            exit = slideOutVertically(settleSpec()) { it } + fadeOut(),
        ) {
            AcceptBar(state = state, onAction = onAction)
        }
    }

    SessionPickerDialogs(state = state, onAction = onAction)

    if (state.showDiscardConfirm) {
        val isGoalSchedule = state.goalId != null
        AwanConfirmDialog(
            title = stringResource(
                if (isGoalSchedule) R.string.ai_tasks_cancel_goal_schedule_title else R.string.ai_tasks_discard_title
            ),
            body = stringResource(
                if (isGoalSchedule) R.string.ai_tasks_cancel_goal_schedule_body else R.string.ai_tasks_discard_body
            ),
            confirmLabel = stringResource(
                if (isGoalSchedule) R.string.ai_tasks_cancel_goal_schedule_confirm else R.string.ai_tasks_discard_confirm
            ),
            confirmVariant = AwanButtonVariant.Destructive,
            onConfirm = { onAction(AiTasksAction.DiscardConfirmed) },
            dismissLabel = stringResource(R.string.ai_tasks_discard_cancel),
            onDismiss = { onAction(AiTasksAction.DiscardCancelled) },
        )
    }
}

/**
 * [Error] carries its own [Error.messageRes] rather than the caller reading `state.errorMessage!!`
 * live — Crossfade keeps the outgoing composition alive for the fade, and by then Retry may already
 * have nulled that field out from under it.
 */
private sealed interface ScreenPhase {
    data object Loading : ScreenPhase
    data class Error(val messageRes: Int) : ScreenPhase
    data object Empty : ScreenPhase
    data object Content : ScreenPhase
}

private fun screenPhase(state: AiTasksState): ScreenPhase = when {
    state.isLoading -> ScreenPhase.Loading
    state.errorMessage != null -> ScreenPhase.Error(state.errorMessage)
    state.isEmptyResult -> ScreenPhase.Empty
    else -> ScreenPhase.Content
}

@Composable
private fun Header(onCancel: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = AwanTheme.spacing.sm, vertical = AwanTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        AwanIconButton(
            onClick = onCancel,
            contentDescription = stringResource(R.string.ai_tasks_cancel_creation),
        ) {
            Icon(
                imageVector = Lucide.X,
                contentDescription = null,
                tint = AwanTheme.colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
        AwanText(stringResource(R.string.ai_tasks_title), style = AwanTheme.styles.titleText)
    }
}

private const val LOADING_LINE_MILLIS = 2200L

/** Beyond this the stagger stops reading as a cascade and starts reading as lag. */
private const val MaxCascadeSteps = 6

/** The screen's one motion curve, already stood down when the user has animations off. */
@Composable
private fun <T> settleSpec(): FiniteAnimationSpec<T> =
    if (reducedMotion()) snap() else AwanTheme.motion.settle.spec()

@Composable
private fun LoadingBody() {
    val reduced = reducedMotion()
    val lines = remember {
        listOf(
            R.string.ai_tasks_loading_1,
            R.string.ai_tasks_loading_2,
            R.string.ai_tasks_loading_3,
            R.string.ai_tasks_loading_4,
        )
    }
    var index by remember { mutableIntStateOf(0) }

    LaunchedEffect(reduced) {
        if (reduced) return@LaunchedEffect
        while (true) {
            delay(LOADING_LINE_MILLIS)
            index = (index + 1) % lines.size
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // No aura here: it strokes the bounding box, and around a mascot that is nowhere near
        // rectangular that reads as a stray outline. Idle already floats under its own steam.
        AwanMascot(expression = MascotExpression.Idle, width = 160.dp)
        Box(modifier = Modifier.padding(top = AwanTheme.spacing.lg)) {
            Crossfade(targetState = index, label = "aiTasksLoadingText") {
                AwanText(stringResource(lines[it]), style = AwanTheme.styles.bodySecondaryText)
            }
        }
    }
}

@Composable
private fun ErrorBody(messageRes: Int, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AwanTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Curious, width = 120.dp)
        AwanText(
            text = stringResource(messageRes),
            style = AwanTheme.styles.bodySecondaryText,
            modifier = Modifier.padding(top = AwanTheme.spacing.md, bottom = AwanTheme.spacing.md),
        )
        AwanButton(onClick = onRetry) { AwanText(stringResource(R.string.ai_tasks_retry)) }
    }
}

@Composable
private fun EmptyBody(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(AwanTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AwanMascot(expression = MascotExpression.Idle, width = 120.dp)
        AwanText(
            text = stringResource(R.string.ai_tasks_empty_title),
            style = AwanTheme.styles.headingText,
            modifier = Modifier.padding(top = AwanTheme.spacing.md),
        )
        AwanText(
            text = stringResource(R.string.ai_tasks_empty_body),
            style = AwanTheme.styles.bodySecondaryText,
            modifier = Modifier.padding(top = AwanTheme.spacing.xxs, bottom = AwanTheme.spacing.md),
        )
        AwanButton(onClick = onRetry) { AwanText(stringResource(R.string.ai_tasks_retry)) }
    }
}

@Composable
private fun ProposalsBody(state: AiTasksState, onAction: (AiTasksAction) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = AwanTheme.spacing.md,
            vertical = AwanTheme.spacing.md,
        ),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
    ) {
        val summary = state.sourceSummary
        if (summary != null) {
            item(key = "source-summary") {
                CascadeItem(index = 0, modifier = Modifier.fillMaxWidth()) {
                    SourceSummaryCard(imageUri = state.imageUri, summary = summary)
                }
            }
        }
        if (state.goalId == null && state.availableGoals.isNotEmpty() && state.proposals.isNotEmpty()) {
            item(key = "bulk-goal-selector") {
                CascadeItem(index = 0, modifier = Modifier.fillMaxWidth()) {
                    BulkGoalSelector(
                        goals = state.availableGoals,
                        commonGoalId = state.commonGoalId,
                        isMixed = state.isMixedGoals,
                        onBulkGoalPicked = { onAction(AiTasksAction.BulkGoalPicked(it)) },
                    )
                }
            }
        }
        itemsIndexed(state.proposals, key = { _, proposal -> proposal.id }) { index, proposal ->
            // Capped so a card scrolled into view later still rises in, without sitting blank for
            // its ordinal's worth of stagger first.
            // animateItem carries the removal and the undo: the gap closes under the cards below
            // rather than snapping shut, and a restored card fades back into the slot it left.
            CascadeItem(
                index = (index + 1).coerceAtMost(MaxCascadeSteps),
                modifier = Modifier
                    .animateItem(placementSpec = settleSpec())
                    .fillMaxWidth(),
            ) {
                ProposalCard(
                    draft = proposal.draft,
                    sessions = proposal.sessions,
                    reason = proposal.reason,
                    isExpanded = proposal.isExpanded,
                    categories = state.availableCategories,
                    goals = state.availableGoals,
                    onRemoved = { onAction(AiTasksAction.Removed(proposal.id)) },
                    onToggleExpanded = { onAction(AiTasksAction.ToggleExpanded(proposal.id)) },
                    onTitleChanged = { onAction(AiTasksAction.TitleChanged(proposal.id, it)) },
                    onDescriptionChanged = { onAction(AiTasksAction.DescriptionChanged(proposal.id, it)) },
                    onDurationPicked = { onAction(AiTasksAction.DurationPicked(proposal.id, it)) },
                    onCategoryPicked = { onAction(AiTasksAction.CategoryPicked(proposal.id, it)) },
                    onGoalPicked = { onAction(AiTasksAction.GoalPicked(proposal.id, it)) },
                    onMandatoryToggled = { onAction(AiTasksAction.MandatoryToggled(proposal.id)) },
                    onSessionTapped = { onAction(AiTasksAction.SessionTapped(proposal.id, it)) },
                    onSessionRemoved = { onAction(AiTasksAction.SessionRemoved(proposal.id, it)) },
                    onSessionAdded = { onAction(AiTasksAction.SessionAdded(proposal.id)) },
                )
            }
        }
        // Leaves room for the sticky accept bar overlaid below.
        item(key = "bottom-spacer") { Box(modifier = Modifier.size(72.dp)) }
    }
}

@Composable
private fun BulkGoalSelector(
    goals: List<Goal>,
    commonGoalId: String?,
    isMixed: Boolean,
    onBulkGoalPicked: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val resolvedGoal = goals.firstOrNull { it.id == commonGoalId }

    AwanCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            AwanText(
                text = stringResource(R.string.ai_tasks_bulk_goal_label),
                style = AwanTheme.styles.metaText,
                modifier = Modifier.weight(1f),
            )

            Box {
                val label = when {
                    isMixed -> stringResource(R.string.ai_tasks_goal_mixed)
                    resolvedGoal != null -> resolvedGoal.title
                    else -> stringResource(R.string.ai_tasks_chip_no_goal)
                }
                AwanChip(
                    label = label,
                    tone = AwanChipTone.Sky,
                    active = resolvedGoal != null || isMixed,
                    onClick = { menuOpen = true },
                )

                AwanDropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    AwanDropdownMenuItem(
                        label = stringResource(R.string.ai_tasks_chip_no_goal),
                        onClick = {
                            onBulkGoalPicked(null)
                            menuOpen = false
                        },
                        selected = !isMixed && commonGoalId == null,
                        leading = { AwanChipDot(tone = AwanChipTone.Sky, active = false) },
                    )
                    goals.forEach { goal ->
                        val active = !isMixed && goal.id == commonGoalId
                        AwanDropdownMenuItem(
                            label = goal.title,
                            onClick = {
                                onBulkGoalPicked(goal.id)
                                menuOpen = false
                            },
                            selected = active,
                            leading = { AwanChipDot(tone = AwanChipTone.Sky, active = active) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceSummaryCard(imageUri: String?, summary: String) {
    var expanded by remember { mutableStateOf(false) }
    val haptics = LocalHapticFeedback.current
    // The whole card is the target; the show more/less line stays as the label for what a tap does.
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            haptics.performHapticFeedback(
                if (expanded) HapticFeedbackType.SegmentTick else HapticFeedbackType.ContextClick,
            )
            expanded = !expanded
        },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
            if (imageUri != null) {
                AwanUriImage(
                    uri = imageUri,
                    contentDescription = stringResource(R.string.ai_tasks_image_content_description),
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            Column {
                AwanText(stringResource(R.string.ai_tasks_source_summary_title), style = AwanTheme.styles.metaText)
                AwanText(
                    text = summary,
                    style = AwanTheme.styles.bodySecondaryText,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                )
                AwanText(
                    text = stringResource(
                        if (expanded) R.string.ai_tasks_source_summary_show_less
                        else R.string.ai_tasks_source_summary_show_more,
                    ),
                    style = AwanTheme.styles.skipLink,
                    modifier = Modifier.padding(top = AwanTheme.spacing.xxs),
                )
            }
        }
    }
}

@Composable
private fun AcceptBar(state: AiTasksState, onAction: (AiTasksAction) -> Unit) {
    val count = state.proposals.size
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AwanTheme.colors.background)
            .padding(AwanTheme.spacing.md),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        AnimatedVisibility(
            visible = state.lastRemoved != null,
            enter = expandVertically(settleSpec()) + fadeIn(),
            exit = shrinkVertically(settleSpec()) + fadeOut(),
        ) {
            // Held rather than read live off state: the exit animation outlives the undo that
            // nulled the field, and the bar must not go blank on its way out.
            val removedTitle = remember(state.lastRemoved) {
                state.lastRemoved?.proposal?.draft?.title.orEmpty()
            }
            UndoRow(title = removedTitle, onUndo = { onAction(AiTasksAction.UndoRemove) })
        }

        // An accept failure belongs next to the button that caused it — swapping the reviewed list
        // for a full-screen error would put the only retry behind a refetch that discards the edits.
        state.acceptError?.let {
            AwanText(text = stringResource(it), style = AwanTheme.styles.errorText)
        }

        AwanButton(
            onClick = { onAction(AiTasksAction.Accept) },
            enabled = count > 0 && !state.isAccepting,
            isLoading = state.isAccepting,
            modifier = Modifier.fillMaxWidth(),
        ) {
            AwanText(
                if (count == 0) {
                    stringResource(R.string.ai_tasks_accept_none)
                } else {
                    pluralStringResource(R.plurals.ai_tasks_accept, count, count)
                },
            )
        }

        AnimatedVisibility(
            visible = state.canReset,
            enter = expandVertically(settleSpec()) + fadeIn(),
            exit = shrinkVertically(settleSpec()) + fadeOut(),
        ) {
            AwanButton(
                onClick = { onAction(AiTasksAction.ResetPlan) },
                variant = AwanButtonVariant.Quiet,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AwanText(stringResource(R.string.ai_tasks_reset_plan))
            }
        }
    }
}

/** One removal deep, so this names the task rather than counting them. */
@Composable
private fun UndoRow(title: String, onUndo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        AwanText(
            text = stringResource(R.string.ai_tasks_removed, title),
            style = AwanTheme.styles.metaText,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false),
        )
        AwanButton(onClick = onUndo, variant = AwanButtonVariant.Quiet) {
            AwanText(stringResource(R.string.ai_tasks_undo))
        }
    }
}

@Composable
private fun SessionPickerDialogs(state: AiTasksState, onAction: (AiTasksAction) -> Unit) {
    val target = state.sessionPicker ?: return
    val session = state.proposals.firstOrNull { it.id == target.proposalId }?.sessions?.getOrNull(target.sessionIndex)
        ?: return

    when (target.step) {
        PickerStep.DATE -> AwanDatePickerDialog(
            initialDate = target.pendingDate ?: session.start.toLocalDate(),
            confirmLabel = stringResource(R.string.ai_tasks_picker_next),
            cancelLabel = stringResource(R.string.ai_tasks_picker_cancel),
            onDismiss = { onAction(AiTasksAction.SessionPickerDismissed) },
            onConfirm = { onAction(AiTasksAction.SessionDatePicked(it)) },
        )

        PickerStep.TIME -> AwanTimePickerDialog(
            initialMinutes = session.start.hour * MinutesPerHour + session.start.minute,
            confirmLabel = stringResource(R.string.ai_tasks_picker_set),
            cancelLabel = stringResource(R.string.ai_tasks_picker_cancel),
            onDismiss = { onAction(AiTasksAction.SessionPickerDismissed) },
            onConfirm = { onAction(AiTasksAction.SessionTimePicked(it)) },
        )
    }
}
