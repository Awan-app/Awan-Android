package com.awan.app.core.model

import java.time.LocalDateTime

/**
 * What the quick-add sheet has collected so far: the parser fills [startAt], [durationMinutes] and
 * [zoneToken] from the typed sentence, the chips let the user override them, and
 * `CreateTaskFromDraftUseCase` turns it into the right API call.
 *
 * A null [startAt] means unscheduled — the task lands in the Inbox with no session.
 */
data class TaskDraft(
    val title: String,
    val description: String? = null,
    val mandatory: Boolean = true,
    val durationMinutes: Int? = null,
    val startAt: LocalDateTime? = null,
    val zoneToken: String? = null,
    val zoneId: String? = null,
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
