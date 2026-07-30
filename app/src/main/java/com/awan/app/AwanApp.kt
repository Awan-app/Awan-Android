package com.awan.app

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
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
import com.awan.app.core.designsystem.AwanBottomNavBar
import com.awan.app.core.designsystem.BottomNavItem
import com.awan.core.navigation.Navigator
import androidx.hilt.navigation.compose.hiltViewModel
import com.awan.feature.addtask.navigation.goalPreviewEntry
import com.awan.feature.addtask.navigation.GoalPreviewRoute
import com.awan.feature.addtask.presentation.AddTaskViewModel
import com.awan.feature.addtask.ui.AddTaskSheet
import com.awan.feature.auth.api.LoginRoute
import com.awan.feature.auth.impl.navigation.authEntry
import com.awan.feature.calendar.impl.navigation.calendarEntry
import com.awan.feature.chat.impl.navigation.chatEntry
import com.awan.feature.goals.api.GoalsRoute
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
    val addTaskViewModel: AddTaskViewModel = hiltViewModel()

    if (showAddTask) {
        AddTaskSheet(
            onDismiss = { showAddTask = false },
            onGoalCreated = { _ ->
                navigator.replaceAll(GoalsRoute)
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        val entryProvider = entryProvider {
            splashEntry(
                onNavigateToNext = { destination ->
                    when (destination) {
                        SplashDestination.Auth -> navigator.replaceAll(LoginRoute)
                        SplashDestination.Onboarding -> navigator.replaceAll(OnboardingRoute)
                        SplashDestination.Home -> navigator.replaceAll(HomeRoute)
                        SplashDestination.Loading -> { /* Keep showing splash */ }
                    }
                }
            )
            authEntry(
                onNavigateToOtp = { email -> navigator.navigate(OtpRoute(email)) },
                onNavigateToHome = { navigator.replaceAll(HomeRoute) },
                onNavigateToOnboarding = { navigator.replaceAll(OnboardingRoute) },
                onPopBackStack = { navigator.goBack() }
            )
            onboardingEntry(
                onComplete = { navigator.replaceAll(HomeRoute) },
                onExit = { navigator.replaceAll(LoginRoute) }
            )
            marketplaceEntry()
            homeEntry(
                onLogout = { navigator.replaceAll(LoginRoute) },
                onNavigateToCalendar = { navigator.navigate(com.awan.feature.calendar.api.CalendarRoute) },
            )
            calendarEntry(
                onDateSelected = { /* consumed within calendar screen */ },
                onBack = { navigator.goBack() },
            )
            chatEntry()
            goalsEntry()
            profileEntry(
                onLogout = { navigator.replaceAll(com.awan.feature.auth.api.LoginRoute) }
            )
            goalPreviewEntry(
                viewModel = addTaskViewModel,
                onBack = { navigator.goBack() },
            )
        }

        BackHandler(
            enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack
        ) {
            navigator.goBack()
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

            AwanBottomNavBar(
                items = navItems,
                selectedItemId = selectedDest?.name,
                onItemSelected = { item ->
                    val dest = appState.topLevelDestinations.find { it.name == item.id }
                    if (dest?.isFab == true) {
                        showAddTask = true
                    } else {
                        dest?.route?.let { route ->
                            navigator.navigate(route)
                        }
                    }
                },
                onFabClick = {
                    showAddTask = true
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}