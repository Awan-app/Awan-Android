package com.awan.feature.addtask.presentation

import androidx.annotation.StringRes
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.parser.TaskInputParser
import com.awan.app.core.model.Category
import com.awan.app.core.model.GoalDecompositionBlock
import com.awan.app.core.model.GoalProposal
import com.awan.app.core.model.TaskDraft
import java.time.LocalDate
import java.time.LocalDateTime

private const val MINUTES_PER_HOUR = 60

enum class AddTaskMode { TASK, GOAL }

enum class AddTaskPicker { DATE, TIME, DURATION, CATEGORY }

/**
 * The four user-visible steps of the AI goal-decomposition flow.
 */
sealed interface GoalStep {
    data object Initial : GoalStep
    data class MultipleChoice(
        val question: String,
        val options: List<String>,
        val selectedOption: String? = null,
    ) : GoalStep

    data class WritingQuestion(
        val question: String,
    ) : GoalStep

    data class Preview(
        val proposal: GoalProposal,
    ) : GoalStep
}

/**
 * What the goal side of the sheet is showing, as opposed to which question it is on. Fully derived
 * from [AddTaskState] — see [AddTaskState.goalPhase].
 */
enum class GoalPhase { Editing, Thinking, PlanReady }

/**
 * What actually got created, held so the sheet can show it back rather than vanishing. [firstSession]
 * is null for an Inbox task — nothing is scheduled, and saying "waiting in your Inbox" is the honest
 * answer rather than inventing a time.
 */
data class TaskConfirmation(
    val title: String,
    val firstSession: LocalDateTime? = null,
    val durationMinutes: Int? = null,
)

data class AddTaskState(
    /** Anchors the "Today"/"Tomorrow" chip labels; supplied by the ViewModel's clock. */
    val today: LocalDate,
    val mode: AddTaskMode = AddTaskMode.TASK,
    val input: String = "",
    val description: String = "",
    val parsed: ParsedTaskInput = ParsedTaskInput.Empty,
    val mandatory: Boolean = true,
    /** The categories offered by the category chip's menu. Not date-scoped, so loaded once. */
    val availableCategories: List<Category> = emptyList(),
    val resolvedCategory: Category? = null,
    val isResolvingCategory: Boolean = false,
    val openPicker: AddTaskPicker? = null,
    /** Chosen on the date step, held until the clock step commits both into the sentence. */
    val pendingDate: LocalDate? = null,
    val goalStep: GoalStep = GoalStep.Initial,
    val goalSessionId: String? = null,
    val goalReplyBlocks: List<GoalDecompositionBlock> = emptyList(),
    /** Switched on, the sentence and note go to Awan's full-screen proposal review instead of being parsed here. */
    val aiEnabled: Boolean = false,
    /** A gallery pick or camera shot, attached to the note as extra context for Awan. */
    val imageUri: String? = null,
    val isSubmitting: Boolean = false,
    /** True for one beat after a successful create, so the mascot can cheer. */
    val isCelebrating: Boolean = false,
    /** Non-null once the task exists: the sheet stops being a form and becomes a receipt. */
    val confirmation: TaskConfirmation? = null,
    val showGoalSaveChoice: Boolean = false,
    val showDiscardConfirm: Boolean = false,
    @StringRes val errorMessage: Int? = null,
    val hasRequestedMicPermission: Boolean = false,
    val goalId: String? = null,
    val zoneId: String? = null,
) {
    /**
     * While the parser is stood down there is no `parsed.title` to check, so the raw text — or a
     * photo with no text at all — stands in for it. Placing a task without Awan means the sentence
     * has to carry the whole placement itself: a title, a clock time and a length. A bare day
     * defaults its hour, which is not a time anyone chose, so it does not count as one.
     */
    val canSubmit: Boolean
        get() = if (mode == AddTaskMode.GOAL) {
            !isSubmitting && when (val step = goalStep) {
                GoalStep.Initial -> input.isNotBlank()
                is GoalStep.MultipleChoice -> !step.selectedOption.isNullOrBlank() || input.isNotBlank()
                is GoalStep.WritingQuestion, is GoalStep.Preview -> input.isNotBlank()
            }
        } else {
            !isSubmitting && if (aiEnabled) {
                input.isNotBlank() || imageUri != null
            } else {
                parsed.title.isNotBlank() && parsed.hasExplicitTime && parsed.durationMinutes != null
            }
        }

    /**
     * Inside the sheet, GOAL + [isSubmitting] can only be an in-flight decomposition: a direct task
     * create is TASK mode, and accepting a proposal happens on the preview route, not here.
     */
    val goalPhase: GoalPhase
        get() = when {
            mode != AddTaskMode.GOAL -> GoalPhase.Editing
            goalStep is GoalStep.Preview -> GoalPhase.PlanReady
            isSubmitting -> GoalPhase.Thinking
            else -> GoalPhase.Editing
        }

    val canAcceptGoal: Boolean
        get() = mode == AddTaskMode.GOAL &&
            goalStep is GoalStep.Preview &&
            goalSessionId != null &&
            !isSubmitting

    val showsAiSwitch: Boolean
        get() = mode == AddTaskMode.TASK && confirmation == null

    /** Same reason: the mode selector would offer a way out of a task that already exists. */
    val showsModeSelector: Boolean
        get() = confirmation == null && !isSubmitting &&
            (mode == AddTaskMode.TASK || (goalSessionId == null && goalStep == GoalStep.Initial))

    /** The chips are a readout of the parser, so they go quiet while Awan is doing the reading instead. */
    val showsAttributeChips: Boolean get() = !aiEnabled

    /** Nothing is at risk once the task is saved, so the receipt closes without an argument. */
    val isDirty: Boolean
        get() = confirmation == null && (
            input.isNotBlank() ||
            description.isNotBlank() ||
            imageUri != null ||
            aiEnabled ||
            (mode == AddTaskMode.GOAL && (goalSessionId != null || goalStep != GoalStep.Initial))
        )

    /** Opens the calendar on whatever day the sentence already says, or on today. */
    val pickerInitialDate: LocalDate
        get() = parsed.startAt?.toLocalDate() ?: today

    /** Opens the clock on whatever the sentence already says, or on the parser's default hour. */
    val pickerInitialMinutes: Int
        get() = parsed.startAt
            ?.takeIf { hasPickableTime }
            ?.let { it.hour * MINUTES_PER_HOUR + it.minute }
            ?: (TaskInputParser.DEFAULT_HOUR * MINUTES_PER_HOUR)

    private val hasPickableTime: Boolean get() = parsed.hasExplicitTime

    /**
     * Awan watches what you type: curious once the sentence carries something schedulable or once
     * it is standing by for Awan, greeting you while it's still just a title, cheering when the task
     * lands. The cheer outlasts [isCelebrating] — that only times the sparkles, and dropping back to
     * a greeting while the receipt is still up would read as Awan losing interest in what it just did.
     */
    val mascot: MascotExpression
        get() = when {
            isCelebrating || confirmation != null -> MascotExpression.Celebrate
            goalPhase == GoalPhase.PlanReady -> MascotExpression.Celebrate
            // Plain, not curious: Awan is working, not asking.
            goalPhase == GoalPhase.Thinking -> MascotExpression.Idle
            mode == AddTaskMode.GOAL -> MascotExpression.Curious
            aiEnabled -> MascotExpression.Curious
            parsed.startAt != null || parsed.categoryToken != null -> MascotExpression.Curious
            input.isNotBlank() -> MascotExpression.Greet
            else -> MascotExpression.Idle
        }

    fun toDraft(): TaskDraft = TaskDraft(
        title = parsed.title,
        description = description.takeIf { it.isNotBlank() },
        mandatory = mandatory,
        durationMinutes = parsed.durationMinutes,
        startAt = parsed.startAt,
        categoryToken = parsed.categoryToken,
        categoryId = resolvedCategory?.id,
        goalId = goalId,
    )
}
