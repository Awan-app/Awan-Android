package com.awan.feature.onboarding.impl.presentation

sealed interface OnboardingEvent {
    /** Onboarding finished — replace the back stack with home. */
    data object NavigateHome : OnboardingEvent

    /** Launch the OS POST_NOTIFICATIONS prompt (API 33+). */
    data object RequestNotificationPermission : OnboardingEvent

    /** First step reached with a Back — exit the flow via the navigator. */
    data object ExitFlow : OnboardingEvent
}
