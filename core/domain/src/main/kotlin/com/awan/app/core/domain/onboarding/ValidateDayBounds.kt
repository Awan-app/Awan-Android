package com.awan.app.core.domain.onboarding

import com.awan.app.core.model.DayBounds
import javax.inject.Inject

sealed interface DayBoundsValidation {
    /** Hard error — wake and sleep are the same minute. Blocks continuing. */
    data object SameTime : DayBoundsValidation

    /** Soft, dismissible warning — waking window shorter than 4 hours. */
    data object ShortWakingWindow : DayBoundsValidation

    data object Valid : DayBoundsValidation
}

/** Pure validation of wake/sleep bounds, correct across midnight. */
class ValidateDayBounds @Inject constructor() {

    operator fun invoke(bounds: DayBounds): DayBoundsValidation = when {
        bounds.wakeMinutes == bounds.sleepMinutes -> DayBoundsValidation.SameTime
        bounds.wakingMinutes < MIN_HEALTHY_WAKING_MINUTES -> DayBoundsValidation.ShortWakingWindow
        else -> DayBoundsValidation.Valid
    }

    private companion object {
        const val MIN_HEALTHY_WAKING_MINUTES = 4 * 60
    }
}
