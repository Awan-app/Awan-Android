package com.awan.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.AwanBottomNavBar
import com.awan.app.core.designsystem.BottomNavItem
import com.awan.core.navigation.Navigator
import com.awan.feature.addtask.ui.AddTaskSheet
import com.awan.feature.auth.api.LoginRoute
import com.awan.feature.auth.impl.navigation.authEntry
import com.awan.feature.calendar.impl.navigation.calendarEntry
import com.awan.feature.chat.impl.navigation.chatEntry
import com.awan.feature.goals.impl.navigation.goalsEntry
import com.awan.feature.home.api.HomeRoute
import com.awan.feature.home.impl.navigation.homeEntry
import com.awan.feature.marketplace.impl.navigation.marketplaceEntry
import com.awan.feature.auth.api.OtpRoute
import com.awan.feature.onboarding.api.OnboardingRoute
import com.awan.feature.onboarding.impl.navigation.onboardingEntry
import com.awan.feature.profile.impl.navigation.profileEntry
import com.awan.feature.splash.impl.navigation.splashEntry
import com.awan.feature.splash.impl.ui.SplashDestination

@Suppress("LongMethod")
@Composable
fun AwanApp(
    appState: AwanAppState,
    modifier: Modifier = Modifier
) {
    val navigator = remember { Navigator(appState.navigationState) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }

    if (showAddTask) {
        AddTaskSheet(onDismiss = { showAddTask = false })
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AwanTheme.colors.background,
        bottomBar = {
            val currentRoute = appState.navigationState.currentKey
            val isTopLevel = appState.topLevelDestinations.any { it.route == currentRoute }
            if (isTopLevel) {
                AwanBottomBar(
                    destinations = appState.topLevelDestinations,
                    currentTopLevelKey = appState.navigationState.currentTopLevelKey,
                    onNavigate = navigator::navigate,
                    onAddTask = { showAddTask = true },
                )
            }
        }
    ) { padding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            color = AwanTheme.colors.background
        ) {
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
                profileEntry(
                    onNavigateToDailyZones = { navigator.navigate(com.awan.feature.profile.api.DailyZonesRoute) },
                    onNavigateToEditRoutine = { id ->
                        navigator.navigate(com.awan.feature.profile.api.EditRoutineRoute(id))
                    },
                    onLogout = { navigator.replaceAll(com.awan.feature.auth.api.LoginRoute) },
                    onBack = { navigator.goBack() }
                )
            }

            Column {
                BackHandler(
                    enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack
                ) {
                    navigator.goBack()
                }
        BackHandler(
            enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack
        ) {
            navigator.goBack()
        }

                NavDisplay(
                    modifier = Modifier.fillMaxSize(),
                    backStack = appState.navigationState.currentSubStack,
                    onBack = { navigator.goBack() },
                    entryProvider = entryProvider
                )
            }
        }
    }
}
        NavDisplay(
            backStack = appState.navigationState.currentSubStack,
            onBack = { navigator.goBack() },
            entryProvider = entryProvider,
            modifier = Modifier.fillMaxSize()
        )

        val currentRoute = appState.navigationState.currentKey
        val isTopLevel = appState.topLevelDestinations.any { dest -> dest.route != null && dest.route == currentRoute }

        if (isTopLevel) {
            val navItems = remember(appState.topLevelDestinations) {
                appState.topLevelDestinations.map { dest ->
                    BottomNavItem(
                        id = dest.name,
                        selectedIcon = dest.selectedIcon,
                        unselectedIcon = dest.unselectedIcon,
                        label = dest.label,
                        isFab = dest.isFab
                    )
                }
            }
            val selectedDest = appState.topLevelDestinations.find { it.route == appState.navigationState.currentTopLevelKey }
