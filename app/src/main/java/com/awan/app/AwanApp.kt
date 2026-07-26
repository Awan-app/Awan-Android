package com.awan.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.awan.core.navigation.Navigator
import com.awan.feature.auth.impl.navigation.authEntry
import com.awan.feature.calendar.impl.navigation.calendarEntry
import com.awan.feature.chat.impl.navigation.chatEntry
import com.awan.feature.goals.impl.navigation.goalsEntry
import com.awan.feature.home.impl.navigation.homeEntry
import com.awan.feature.onboarding.impl.navigation.onboardingEntry
import com.awan.feature.profile.impl.navigation.profileEntry
import com.awan.feature.profile_setup.impl.navigation.profileSetupEntry
import com.awan.feature.splash.impl.navigation.splashEntry

import com.awan.feature.splash.impl.ui.SplashDestination

@Suppress("LongMethod")
@Composable
fun AwanApp(
    appState: AwanAppState,
    modifier: Modifier = Modifier
) {
    val navigator = remember { Navigator(appState.navigationState) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val currentRoute = appState.navigationState.currentKey
            val isTopLevel = appState.topLevelDestinations.any { it.route == currentRoute }
            if (isTopLevel) {
                NavigationBar {
                    appState.topLevelDestinations.forEach { destination ->
                        NavigationBarItem(
                            selected = appState.navigationState.currentTopLevelKey == destination.route,
                            onClick = { navigator.navigate(destination.route) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = { Text(destination.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            val entryProvider = entryProvider {
                splashEntry(
                    onNavigateToNext = { destination ->
                        when (destination) {
                            SplashDestination.Auth -> navigator.replaceAll(com.awan.feature.auth.api.LoginRoute)
                            SplashDestination.Onboarding -> navigator.replaceAll(com.awan.feature.onboarding.api.OnboardingRoute)
                            SplashDestination.Home -> navigator.replaceAll(com.awan.feature.home.api.HomeRoute)
                            SplashDestination.Loading -> { /* Keep showing splash */ }
                        }
                    }
                )
                authEntry(
                    onNavigateToOtp = { email -> navigator.navigate(com.awan.feature.auth.api.OtpRoute(email)) },
                    onNavigateToHome = { navigator.replaceAll(com.awan.feature.home.api.HomeRoute) },
                    onNavigateToOnboarding = { navigator.replaceAll(com.awan.feature.onboarding.api.OnboardingRoute) },
                    onPopBackStack = { navigator.goBack() }
                )
                onboardingEntry(
                    onComplete = { navigator.replaceAll(com.awan.feature.home.api.HomeRoute) },
                    onExit = { navigator.replaceAll(com.awan.feature.auth.api.LoginRoute) }
                )
                profileSetupEntry(
                    onNavigateToHome = { navigator.replaceAll(com.awan.feature.home.api.HomeRoute) }
                )
                homeEntry(
                    onLogout = { navigator.replaceAll(com.awan.feature.auth.api.LoginRoute) },
                    onNavigateToCalendar = { navigator.navigate(com.awan.feature.calendar.api.CalendarRoute) },
                )
                calendarEntry()
                chatEntry()
                goalsEntry()
                profileEntry()
            }

            BackHandler(
                enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack
            ) {
                navigator.goBack()
            }

            NavDisplay(
                backStack = appState.navigationState.currentSubStack,
                onBack = { navigator.goBack() },
                entryProvider = entryProvider
            )
        }
    }
}
