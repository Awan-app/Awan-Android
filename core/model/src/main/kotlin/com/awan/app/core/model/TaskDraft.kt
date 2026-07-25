package com.awan.app.core.model

import java.time.LocalDateTime

/**
 * What the quick-add sheet has collected so far: the parser fills [startAt], [durationMinutes] and
 * [categoryToken] from the typed sentence, the chips let the user override them, and
 * `CreateTaskUseCase` turns it into the right API call.
 *
 * A null [startAt] means unscheduled — the task lands in the Inbox with no session. There is no
 * `zoneId` here on purpose: the draft names a category, and the session's zone is resolved from it
 * against the chosen day inside `CreateTaskUseCase`.
 */
data class TaskDraft(
    val title: String,
    val description: String? = null,
    val mandatory: Boolean = true,
    val durationMinutes: Int? = null,
    val startAt: LocalDateTime? = null,
    val categoryToken: String? = null,
    val categoryId: String? = null,
    val estimatedPoints: Int = 0,
    val allowTaskSplitting: Boolean = false,
    val goalId: String? = null,
) {
    val isValid: Boolean get() = title.isNotBlank()

    companion object {
        /**
         * Used when the sentence names a time but no length. The user's `preferredSessionDuration`
         * is the real answer.
         *
         * ponytail: constant fallback; read the profile's preferredSessionDuration once a profile
         * use case exists.
         */
        const val DEFAULT_DURATION_MINUTES = 60
    }
}
