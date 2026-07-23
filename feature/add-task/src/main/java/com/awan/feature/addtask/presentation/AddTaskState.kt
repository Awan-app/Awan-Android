package com.awan.feature.addtask.presentation

import androidx.annotation.StringRes
import com.awan.app.core.designsystem.MascotExpression
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.parser.TaskInputParser
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.TaskDraft
import java.time.LocalDate

private const val MINUTES_PER_HOUR = 60

enum class AddTaskMode { TASK, GOAL }

enum class AddTaskPicker { TIME, DURATION }

data class AddTaskState(
    /** Anchors the "Today"/"Tomorrow" chip labels; supplied by the ViewModel's clock. */
    val today: LocalDate,
    val mode: AddTaskMode = AddTaskMode.TASK,
    val input: String = "",
    val description: String = "",
    val parsed: ParsedTaskInput = ParsedTaskInput.Empty,
    val mandatory: Boolean = true,
    val resolvedZone: DayZone? = null,
    val isResolvingZone: Boolean = false,
    val openPicker: AddTaskPicker? = null,
    val isSubmitting: Boolean = false,
    /** True for one beat after a successful create, so the mascot can cheer before the sheet goes. */
    val isCelebrating: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val canSubmit: Boolean
        get() = mode == AddTaskMode.TASK && parsed.title.isNotBlank() && !isSubmitting

    /** Opens the clock on whatever the sentence already says, or on the parser's default hour. */
    val pickerInitialMinutes: Int
        get() = parsed.startAt
            ?.takeIf { hasPickableTime }
            ?.let { it.hour * MINUTES_PER_HOUR + it.minute }
            ?: (TaskInputParser.DEFAULT_HOUR * MINUTES_PER_HOUR)

    private val hasPickableTime: Boolean get() = parsed.hasExplicitTime

    /**
     * Awan watches what you type: curious once the sentence carries something schedulable, greeting
     * you while it's still just a title, cheering when the task lands.
     */
    val mascot: MascotExpression
        get() = when {
            isCelebrating -> MascotExpression.Celebrate
            mode == AddTaskMode.GOAL -> MascotExpression.Curious
            parsed.startAt != null || parsed.zoneToken != null -> MascotExpression.Curious
            input.isNotBlank() -> MascotExpression.Greet
            else -> MascotExpression.Idle
        }

    /** The zone token matched a real zone, so its id can go on the session. */
    val hasUnknownZone: Boolean
        get() = parsed.zoneToken != null && resolvedZone == null && !isResolvingZone

    fun toDraft(): TaskDraft = TaskDraft(
        title = parsed.title,
        description = description.takeIf { it.isNotBlank() },
        mandatory = mandatory,
        durationMinutes = parsed.durationMinutes,
        startAt = parsed.startAt,
        zoneToken = parsed.zoneToken,
        zoneId = resolvedZone?.id,
    )
}
