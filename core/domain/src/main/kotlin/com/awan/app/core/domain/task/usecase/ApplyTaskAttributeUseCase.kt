package com.awan.app.core.domain.task.usecase

import com.awan.app.core.domain.task.parser.ParsedTaskInput
import com.awan.app.core.domain.task.parser.TaskInputWriter
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject

/** An attribute picked from a chip rather than typed. */
sealed interface TaskAttribute {
    data class At(val moment: LocalDateTime) : TaskAttribute
    data class Lasting(val minutes: Int) : TaskAttribute
}

/**
 * Folds a chip's choice back into the typed sentence and returns the new sentence. The caller then
 * re-parses it like any other keystroke, so there is exactly one path from text to draft.
 */
class ApplyTaskAttributeUseCase @Inject constructor(
    private val clock: Clock,
) {
    operator fun invoke(input: String, parsed: ParsedTaskInput, attribute: TaskAttribute): String {
        val today = LocalDate.now(clock)
        return when (attribute) {
            is TaskAttribute.At -> TaskInputWriter.withTime(input, parsed, attribute.moment, today)
            is TaskAttribute.Lasting -> TaskInputWriter.withDuration(input, parsed, attribute.minutes)
        }
    }
}
