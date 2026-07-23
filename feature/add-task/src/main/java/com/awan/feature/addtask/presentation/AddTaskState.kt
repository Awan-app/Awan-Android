package com.awan.feature.addtask.presentation

import androidx.annotation.StringRes
import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.model.DayZone
import com.awan.app.core.model.TaskDraft
import java.time.LocalDate

enum class AddTaskMode { TASK, GOAL }

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
    val isSubmitting: Boolean = false,
    @StringRes val errorMessage: Int? = null,
) {
    val canSubmit: Boolean
        get() = mode == AddTaskMode.TASK && parsed.title.isNotBlank() && !isSubmitting

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
