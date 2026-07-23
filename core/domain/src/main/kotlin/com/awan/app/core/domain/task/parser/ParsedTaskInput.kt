package com.awan.app.core.domain.task.parser

import java.time.LocalDateTime

enum class TaskTokenKind { DATE_TIME, DURATION, ZONE }

/** Where a recognised token sits in the *raw* input, so the field can highlight it in place. */
data class TaskToken(
    val range: IntRange,
    val kind: TaskTokenKind,
)

data class ParsedTaskInput(
    val title: String,
    val startAt: LocalDateTime? = null,
    val durationMinutes: Int? = null,
    val zoneToken: String? = null,
    val tokens: List<TaskToken> = emptyList(),
) {
    companion object {
        val Empty = ParsedTaskInput(title = "")
    }
}
