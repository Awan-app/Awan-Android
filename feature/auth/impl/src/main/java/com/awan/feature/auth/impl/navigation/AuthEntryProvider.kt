package com.awan.feature.auth.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.auth.api.LoginRoute
import com.awan.feature.auth.api.OtpRoute
import com.awan.feature.auth.impl.ui.email.EmailRouteScreen
import com.awan.feature.auth.impl.ui.otp.OtpRouteScreen

fun EntryProviderScope<Route>.authEntry(
    onNavigateToOtp: (email: String) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToOnboarding: () -> Unit,
    onPopBackStack: () -> Unit,
) {
    entry<LoginRoute> {
        EmailRouteScreen(
            onNext = { email -> onNavigateToOtp(email) },
        )
    }

    entry<OtpRoute> { entry ->
        OtpRouteScreen(
            email = entry.email,
            onNavigateToHome = onNavigateToHome,
            onNavigateToOnboarding = onNavigateToOnboarding,
            onBack = onPopBackStack,
            onUseDifferentEmail = onPopBackStack,
        )
    }
}
