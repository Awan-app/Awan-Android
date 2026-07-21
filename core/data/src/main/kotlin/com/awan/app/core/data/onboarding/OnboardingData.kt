package com.awan.app.core.data.onboarding

import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.UserProfile
import com.awan.app.core.model.Zone

/**
 * The full onboarding draft. Everything above the repository observes this; nothing here is
 * placement logic — the zones/first task are stored exactly as the domain layer produced them.
 */
data class OnboardingData(
    val profile: UserProfile? = null,
    val bounds: DayBounds = DayBounds.Default,
    val zones: List<Zone> = Zone.defaults,
    val preferredTaskLengthMinutes: Int = DEFAULT_TASK_LENGTH_MINUTES,
    val firstTask: FirstTask? = null,
    val completed: Boolean = false,
) {
    companion object {
        const val DEFAULT_TASK_LENGTH_MINUTES = 60
    }
}
