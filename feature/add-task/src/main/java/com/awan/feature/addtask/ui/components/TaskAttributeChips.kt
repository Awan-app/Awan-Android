package com.awan.feature.addtask.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanChip
import com.awan.app.core.designsystem.AwanChipDot
import com.awan.app.core.designsystem.AwanChipTone
import com.awan.app.core.designsystem.AwanDropdownMenu
import com.awan.app.core.designsystem.AwanDropdownMenuItem
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.Category
import com.awan.feature.addtask.R
import com.awan.feature.addtask.presentation.AddTaskPicker
import com.awan.feature.addtask.presentation.AddTaskState
import java.time.LocalDate

/** The lengths worth one tap; anything else is typed into the sentence directly. */
private val DurationPresets = listOf(15, 30, 45, 60, 90, 120, 180, 240)

/**
 * A live readout of the sentence. Every chip here except Mandatory is derived from the typed text,
 * and the two editable ones write their choice *back* into that text — so the sentence stays the
 * one place a draft is defined, and the chips never hold state of their own.
 *
 * Wrapping rather than scrolling: the toggle is the last chip, and a chip parked off the right edge
 * is a chip nobody knows is there.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskAttributeChips(
    state: AddTaskState,
    today: LocalDate,
    onEditWhen: () -> Unit,
    onEditDuration: () -> Unit,
    onDurationPicked: (Int) -> Unit,
    onDurationMenuDismissed: () -> Unit,
    onEditCategory: () -> Unit,
    onCategoryPicked: (String) -> Unit,
    onCategoryMenuDismissed: () -> Unit,
    onToggleMandatory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs),
    ) {
        AwanChip(
            label = whenChipLabel(state, today),
            tone = AwanChipTone.Sky,
            active = state.parsed.startAt != null,
            onClick = onEditWhen,
        )

        Box {
            AwanChip(
                label = state.parsed.durationMinutes?.let { durationLabel(it) }
                    ?: stringResource(R.string.add_task_chip_no_duration),
                tone = AwanChipTone.Violet,
                active = state.parsed.durationMinutes != null,
                onClick = onEditDuration,
            )

            DurationMenu(
                expanded = state.openPicker == AddTaskPicker.DURATION,
                selectedMinutes = state.parsed.durationMinutes,
                onDismiss = onDurationMenuDismissed,
                onSelect = onDurationPicked,
            )
        }

        Box {
            CategoryChip(state = state, onClick = onEditCategory)

            CategoryMenu(
                expanded = state.openPicker == AddTaskPicker.CATEGORY,
                categories = state.availableCategories,
                selectedCategoryId = state.resolvedCategory?.id,
                onDismiss = onCategoryMenuDismissed,
                onSelect = onCategoryPicked,
            )
        }

        MandatoryToggle(mandatory = state.mandatory, onToggle = onToggleMandatory)
    }
}

@Composable
private fun whenChipLabel(state: AddTaskState, today: LocalDate): String {
    val startAt = state.parsed.startAt ?: return stringResource(R.string.add_task_chip_today_no_time)
    return rememberWhenLabel(startAt, today, showTime = state.parsed.hasExplicitTime)
}

/**
 * Lengths hang off the chip they set, not out of a dialog in the middle of the screen — the choice
 * is small enough that leaving the sentence for it costs more than it's worth.
 */
@Composable
private fun DurationMenu(
    expanded: Boolean,
    selectedMinutes: Int?,
    onDismiss: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    AwanDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DurationPresets.forEach { minutes ->
            val active = minutes == selectedMinutes
            AwanDropdownMenuItem(
                label = durationLabel(minutes),
                onClick = { onSelect(minutes) },
                selected = active,
                leading = { AwanChipDot(tone = AwanChipTone.Violet, active = active) },
            )
        }
    }
}

/**
 * Always present, like the when and length chips: it names the resolved category, or reads
 * "No category" when the sentence hasn't stated one. A `@token` that matched nothing is the one loud
 * state — it is a typo the user can see and fix, so it wears the destructive tone rather than the
 * lavender.
 */
@Composable
private fun CategoryChip(state: AddTaskState, onClick: () -> Unit) {
    val token = state.parsed.categoryToken
    val colors = AwanTheme.colors
    when {
        state.resolvedCategory != null -> AwanChip(
            label = state.resolvedCategory.name,
            tone = AwanChipTone.Violet,
            onClick = onClick,
        )

        state.isResolvingCategory -> AwanChip(
            label = stringResource(R.string.add_task_chip_category_resolving),
            tone = AwanChipTone.Violet,
            active = false,
            onClick = onClick,
        )

        token != null -> AwanChip(
            label = stringResource(R.string.add_task_chip_category_unknown, token),
            tone = AwanChipTone.Neutral,
            active = false,
            onClick = onClick,
        )

        else -> AwanChip(
            label = stringResource(R.string.add_task_chip_no_category),
            tone = AwanChipTone.Violet,
            active = false,
            onClick = onClick,
        )
    }
}

/** The user's categories as one-tap rows, written back into the sentence as `@category`. */
@Composable
private fun CategoryMenu(
    expanded: Boolean,
    categories: List<Category>,
    selectedCategoryId: String?,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
) {
    AwanDropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (categories.isEmpty()) {
            // Tapping a chip that opens an empty menu reads as broken; say why instead.
            AwanDropdownMenuItem(
                label = stringResource(R.string.add_task_category_menu_empty),
                onClick = {},
                enabled = false,
            )
            return@AwanDropdownMenu
        }
        categories.forEach { category ->
            val active = category.id == selectedCategoryId
            AwanDropdownMenuItem(
                label = category.name,
                onClick = { onSelect(category.name) },
                selected = active,
                leading = { AwanChipDot(tone = AwanChipTone.Violet, active = active) },
            )
        }
    }
}
