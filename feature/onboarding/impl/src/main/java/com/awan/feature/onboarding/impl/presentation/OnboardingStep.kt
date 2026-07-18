package com.awan.feature.onboarding.impl.presentation

/** Welcome is an intro screen (no progress dot); Name..Notifications are the 6 dotted steps. */
enum class OnboardingStep {
    Welcome,
    Name,
    DayBounds,
    Zones,
    TaskLength,
    FirstTask,
    Notifications;

    /** 1-based position among the 6 dotted steps, or 0 for [Welcome]. */
    val dotIndex: Int get() = if (this == Welcome) 0 else ordinal

    companion object {
        const val DOT_COUNT = 6
    }
}
