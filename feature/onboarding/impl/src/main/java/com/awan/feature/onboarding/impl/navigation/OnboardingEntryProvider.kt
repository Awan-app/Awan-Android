package com.awan.feature.onboarding.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.onboarding.api.OnboardingRoute
import com.awan.feature.onboarding.impl.ui.OnboardingRoot

fun EntryProviderScope<Route>.onboardingEntry(
    onComplete: () -> Unit,
    onExit: () -> Unit,
) {
    entry<OnboardingRoute> {
        OnboardingRoot(onComplete = onComplete, onExit = onExit)
    }
}
