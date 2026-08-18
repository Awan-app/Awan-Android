package com.awan.feature.aitasks.impl.presentation

import androidx.annotation.StringRes
import com.awan.app.core.model.Category
import com.awan.app.core.model.Goal
import com.awan.app.core.model.ProposedSession
import com.awan.app.core.model.TaskDraft
import java.time.LocalDate

import com.awan.app.core.model.ScheduleOverlapInfo

/** Which picker is open, and for which task/session it's editing — there is only ever one at a time. */
data class SessionPickerTarget(
    val proposalId: Int,
    val sessionIndex: Int,
    val step: PickerStep = PickerStep.DATE,
    val pendingDate: LocalDate? = null,
)

enum class PickerStep { DATE, TIME }

data class ProposalUi(
    val id: Int,
    val draft: TaskDraft,
    val sessions: List<ProposedSession> = emptyList(),
    val reason: String? = null,
    val taskId: String? = null,
    val overlapInfo: ScheduleOverlapInfo? = null,
    val isUnscheduled: Boolean = false,
    /** Collapsed shows a read-only summary; expanded reveals the editable fields and chip menus. */
    val isExpanded: Boolean = false,
)

/** The one removal that can still be taken back, held with the slot it came out of. */
data class RemovedProposal(val proposal: ProposalUi, val index: Int)

data class AiTasksState(
    val isLoading: Boolean = true,
    val goalId: String? = null,
    val imageUri: String? = null,
    val sourceSummary: String? = null,
    val availableCategories: List<Category> = emptyList(),
    val availableGoals: List<Goal> = emptyList(),
    val proposals: List<ProposalUi> = emptyList(),
    /** What Awan first sent back, untouched, so [canReset] has something to restore to. */
    val originalProposals: List<ProposalUi> = emptyList(),
    val lastRemoved: RemovedProposal? = null,
    val sessionPicker: SessionPickerTarget? = null,
    val isAccepting: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    /**
     * Kept apart from [errorMessage]: a failed accept must not replace the reviewed list with the
     * full-screen error, or the retry there would refetch and throw away every edit.
     */
    @StringRes val acceptError: Int? = null,
    val showDiscardConfirm: Boolean = false,
) {
    val isEmptyResult: Boolean get() = !isLoading && errorMessage == null && originalProposals.isEmpty()

    /**
     * Expansion is ignored: opening a card to read it is not a change to the plan, and offering to
     * "reset" after a tap that edited nothing reads as a bug.
     */
    val canReset: Boolean get() = proposals.map { it.copy(isExpanded = false) } != originalProposals

    val isDirty: Boolean get() = goalId != null || proposals.isNotEmpty()

    val commonGoalId: String? get() {
        if (proposals.isEmpty()) return null
        val first = proposals.first().draft.goalId
        return if (proposals.all { it.draft.goalId == first }) first else null
    }

    val isMixedGoals: Boolean get() {
        if (proposals.size <= 1) return false
        val first = proposals.first().draft.goalId
        return proposals.any { it.draft.goalId != first }
    }
}
