package com.awan.app.core.domain.task.parser

import java.time.LocalDateTime

enum class TaskTokenKind { DATE_TIME, DURATION, CATEGORY }

/**
 * A recognised phrase in the *raw* input.
 *
 * [range] is everything the phrase consumed, and is what gets stripped from the title.
 * [highlights] are the parts worth tinting — usually the whole range, but a phrase like
 * `from 3pm to 5pm` tints only `3pm` and `5pm` and leaves the joining words plain.
 */
data class TaskToken(
    val range: IntRange,
    val kind: TaskTokenKind,
    val highlights: List<IntRange> = listOf(range),
)

data class ParsedTaskInput(
    val title: String,
    val startAt: LocalDateTime? = null,
    val durationMinutes: Int? = null,
    val categoryToken: String? = null,
    val tokens: List<TaskToken> = emptyList(),
    /** The sentence named an actual clock time, rather than a bare day that defaulted to one. */
    val hasExplicitTime: Boolean = false,
) {
    /**
     * All tokens of one kind, in input order. A date/time is routinely spread across more than one
     * token — `today at 3pm` is a day token plus a clock token — so anything rewriting the sentence
     * has to account for every one of them, not just the first.
     */
    fun tokensOf(kind: TaskTokenKind): List<TaskToken> = tokens.filter { it.kind == kind }

    companion object {
        val Empty = ParsedTaskInput(title = "")
    }
}
