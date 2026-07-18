package com.awan.feature.onboarding.impl.presentation

sealed interface OnboardingAction {
    data class NameChanged(val firstName: String, val lastName: String) : OnboardingAction
    data class WakeChanged(val minutes: Int) : OnboardingAction
    data class SleepChanged(val minutes: Int) : OnboardingAction
    data object DismissWakingWarning : OnboardingAction
    data object UseSuggestedZones : OnboardingAction
    data class EditZoneWindow(val zoneId: String, val startMinutes: Int, val endMinutes: Int) : OnboardingAction
    data class ReorderZone(val fromIndex: Int, val toIndex: Int) : OnboardingAction
    data class ToggleZoneEnabled(val zoneId: String) : OnboardingAction
    data class TaskLengthChanged(val minutes: Int) : OnboardingAction
    data class FirstTaskTitleChanged(val title: String) : OnboardingAction
    data object SubmitFirstTask : OnboardingAction
    data object EnableNotifications : OnboardingAction
    data class NotificationPermissionResult(val granted: Boolean) : OnboardingAction
    data object NotificationsPermanentlyDenied : OnboardingAction
    data object Next : OnboardingAction
    data object Skip : OnboardingAction
    data object Back : OnboardingAction
}
