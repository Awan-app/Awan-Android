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
import androidx.compose.ui.graphics.Color
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.awan.core.navigation.Navigator
import com.awan.feature.auth.api.LoginRoute
import com.awan.feature.auth.api.OtpRoute
import com.awan.feature.auth.impl.navigation.authEntry
import com.awan.feature.calendar.api.CalendarRoute
import com.awan.feature.calendar.impl.navigation.calendarEntry
import com.awan.feature.chat.impl.navigation.chatEntry
import com.awan.feature.goals.impl.navigation.goalsEntry
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.navigation.homeEntry
import com.awan.feature.onboarding.api.OnboardingRoute
import com.awan.feature.onboarding.impl.navigation.onboardingEntry
import com.awan.feature.profile.impl.navigation.profileEntry
import com.awan.feature.profile_setup.impl.navigation.profileSetupEntry
import com.awan.feature.splash.impl.navigation.splashEntry

@Composable
fun AwanApp(appState: AwanAppState, modifier: Modifier = Modifier) {
    val navigator = remember { Navigator(appState.navigationState) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            val isTopLevel = appState.topLevelDestinations.any { it.route == appState.navigationState.currentKey }
            if (isTopLevel) NavigationBar {
                appState.topLevelDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = appState.navigationState.currentTopLevelKey == destination.route,
                        onClick = { navigator.navigate(destination.route) },
                        icon = { Icon(destination.icon, destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            val entries = entryProvider {
                splashEntry { loggedIn -> navigator.replaceAll(if (loggedIn) HomeRoute() else LoginRoute) }
                authEntry(
                    onNavigateToOtp = { navigator.navigate(OtpRoute(it)) },
                    onNavigateToHome = { navigator.replaceAll(HomeRoute()) },
                    onNavigateToOnboarding = { navigator.replaceAll(OnboardingRoute) },
                    onPopBackStack = navigator::goBack,
                )
                onboardingEntry(onComplete = { navigator.replaceAll(HomeRoute()) }, onExit = { navigator.replaceAll(LoginRoute) })
                profileSetupEntry(onNavigateToHome = { navigator.replaceAll(HomeRoute()) })
                homeEntry(
                    onLogout = { navigator.replaceAll(LoginRoute) },
                    onOpenCalendar = { date -> navigator.navigate(CalendarRoute(date)) },
                )
                calendarEntry(
                    onDateSelected = { _ -> },
                    onBack = navigator::goBack,
                )
                chatEntry()
                goalsEntry()
                profileEntry()
            }
            BackHandler(enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack) { navigator.goBack() }
            NavDisplay(backStack = appState.navigationState.currentSubStack, onBack = navigator::goBack, entryProvider = entries)
        }
    }
}