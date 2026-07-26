package com.awan.feature.onboarding.impl.presentation

import com.awan.app.core.common.text.UiText
import com.awan.app.core.data.onboarding.OnboardingData
import com.awan.app.core.model.DayBounds
import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.Zone
import com.awan.app.core.domain.onboarding.DayBoundsValidation

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val firstName: String = "",
    val lastName: String = "",
    val bounds: DayBounds = DayBounds.Default,
    val boundsValidation: DayBoundsValidation = DayBoundsValidation.Valid,
    val wakingWarningDismissed: Boolean = false,
    val zones: List<Zone> = emptyList(),
    /** The same zones as the server created them — a scheduled session's `zoneId` points here. */
    val templateZones: List<Zone> = emptyList(),
    val overlappingZoneIds: Set<String> = emptySet(),
    val preferredTaskLengthMinutes: Int = OnboardingData.DEFAULT_TASK_LENGTH_MINUTES,
    val firstTaskTitle: String = "",
    val firstTask: FirstTask? = null,
    val firstTaskError: UiText? = null,
    val isSubmittingTask: Boolean = false,
    /** Why the backend account setup did not land. Set means the flow cannot be left yet. */
    val setupError: UiText? = null,
    val celebrateTask: Boolean = false,
    val notificationsPermanentlyDenied: Boolean = false,
) {
    val trimmedFirstName: String get() = firstName.trim()

    val canContinueName: Boolean get() = trimmedFirstName.isNotEmpty()

    val canContinueBounds: Boolean get() = boundsValidation != DayBoundsValidation.SameTime

    val canSubmitFirstTask: Boolean get() = firstTaskTitle.trim().isNotEmpty() && !isSubmittingTask

    val showWakingWarning: Boolean
        get() = boundsValidation == DayBoundsValidation.ShortWakingWindow && !wakingWarningDismissed

    val dayPreview: DayPreviewModel get() = DayPreviewModel.from(bounds, zones, firstTask, templateZones)

    companion object {
        val TASK_LENGTH_OPTIONS = listOf(30, 45, 60, 90, 120, 180)
    }
}
