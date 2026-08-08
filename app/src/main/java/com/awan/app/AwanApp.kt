package com.awan.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.awan.app.core.common.R as CommonR
import com.awan.app.core.designsystem.AwanBottomNavBar
import com.awan.app.core.designsystem.BottomNavItem
import com.awan.app.core.designsystem.ObserveAsEvents
import com.awan.core.navigation.NavigationState
import com.awan.core.navigation.Navigator
import com.awan.core.navigation.Route
import com.awan.feature.addtask.navigation.GoalPreviewRoute
import com.awan.feature.addtask.navigation.goalPreviewEntry
import com.awan.feature.addtask.ui.AddTaskSheet
import com.awan.feature.aitasks.api.AiTaskProposalsRoute
import com.awan.feature.aitasks.impl.navigation.aiTasksEntry
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
import com.awan.feature.profile.api.CategoryManagementRoute
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.impl.navigation.profileEntry
import com.awan.feature.splash.impl.navigation.splashEntry
import com.awan.feature.splash.impl.ui.SplashDestination
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Decorates every sub-stack, not just the visible one.
 *
 * `NavDisplay`'s own default is a `SaveableStateHolder` and nothing else, which leaves
 * `LocalViewModelStoreOwner` pointing at the Activity: every screen's ViewModel then outlives its
 * entry, so popping a destination and navigating back to it hands over the previous visit's state
 * — old results, dialogs still open, `LaunchedEffect` loads skipped because the flag says they ran.
 * [rememberViewModelStoreNavEntryDecorator] scopes the store to the entry instead, and clears it
 * when the entry is popped.
 *
 * Each stack gets its own decorators and all of them are decorated on every recomposition, so
 * switching tabs — which swaps which stack is displayed, not what's in the others — leaves the
 * background tabs' ViewModels alive.
 *
 * Keeping them alive across tabs is the point; keeping them alive across *sessions* is not. A tab
 * root never leaves its own sub-stack, so `onPop` never fires for it and its store would outlive a
 * logout — handing the next user the previous user's ViewModel. Keying on [NavigationState.generation],
 * which every `replaceAll` bumps, drops each stack's decorators at those boundaries; their
 * `rememberViewModelStoreProvider` clears all of its keys on dispose. Configuration changes are
 * unaffected: that path checks the parent lifecycle and deliberately skips the clear.
 */
@Composable
private fun NavigationState.rememberDecoratedEntries(
    entryProvider: (Route) -> NavEntry<Route>,
): List<NavEntry<Route>> {
    val decoratedStacks = subStacks.mapValues { (topLevelKey, stack) ->
        key(topLevelKey, generation) {
            rememberDecoratedNavEntries(
                backStack = stack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider,
            )
        }
    }
    return decoratedStacks[currentTopLevelKey].orEmpty()
}

@Suppress("LongMethod")
@Composable
fun AwanApp(
    appState: AwanAppState,
    sessionExpiredEvents: Flow<Unit>,
    modifier: Modifier = Modifier,
) {
    val navigator = remember { Navigator(appState.navigationState) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    // Holds the live HomeViewModel's selectDate so Calendar can invoke it after popping.
    val homeSelectDateRef = remember { arrayOf<((LocalDate) -> Unit)?>(null) }

    val context = LocalContext.current
    ObserveAsEvents(sessionExpiredEvents) {
        Toast.makeText(context, CommonR.string.error_unauthorized, Toast.LENGTH_LONG).show()
        showAddTask = false
        navigator.replaceAll(LoginRoute)
    }

    if (showAddTask) {
        AddTaskSheet(
            onDismiss = { showAddTask = false },
            onNavigateToGoalPreview = {
                showAddTask = false
                navigator.navigate(GoalPreviewRoute)
            },
            onAiRequested = { text, note, imageUri ->
                showAddTask = false
                navigator.navigate(AiTaskProposalsRoute(text = text, note = note, imageUri = imageUri))
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
                        SplashDestination.Home -> navigator.replaceAll(HomeRoute())
                        SplashDestination.Loading -> { /* Keep showing splash */ }
                    }
                }
            )
            authEntry(
                onNavigateToOtp = { email -> navigator.navigate(OtpRoute(email)) },
                onNavigateToHome = { navigator.replaceAll(HomeRoute()) },
                onNavigateToOnboarding = { navigator.replaceAll(OnboardingRoute) },
                onPopBackStack = { navigator.goBack() }
            )
            onboardingEntry(
                onComplete = { navigator.replaceAll(HomeRoute()) },
                onExit = { navigator.replaceAll(LoginRoute) }
            )
            marketplaceEntry()
            homeEntry(
                onLogout = { navigator.replaceAll(LoginRoute) },
                onNavigateToCalendar = { navigator.navigate(com.awan.feature.calendar.api.CalendarRoute()) },
                onRegisterSelectDate = { fn -> homeSelectDateRef[0] = fn },
            )
            calendarEntry(
                onDateSelected = { date ->
                    navigator.goBack()
                    homeSelectDateRef[0]?.invoke(date)
                },
                onBack = { navigator.goBack() },
            )
            chatEntry()
            goalsEntry()
            aiTasksEntry(onBack = { navigator.goBack() })
            profileEntry(
                onNavigateToDailyZones = { navigator.navigate(DailyZonesRoute) },
                onNavigateToEditRoutine = { templateId -> navigator.navigate(EditRoutineRoute(templateId)) },
                onLogout = { navigator.replaceAll(LoginRoute) },
                onBack = { navigator.goBack()},
                onNavigateToCategoryManagement = { navigator.navigate(CategoryManagementRoute) },
            )
            goalPreviewEntry(
                onBack = { navigator.goBack() },
                onNavigateToGoals = { navigator.replaceAll(GoalsRoute) },
            )
        }

        BackHandler(
            enabled = appState.navigationState.canGoBackTopLevel && !appState.navigationState.canGoBackSubStack
        ) {
            navigator.goBack()
        }

        NavDisplay(
            entries = appState.navigationState.rememberDecoratedEntries(entryProvider),
            onBack = { navigator.goBack() },
            modifier = Modifier.fillMaxSize()
        )

        val currentRoute = appState.navigationState.currentKey
        val isTopLevel = appState.topLevelDestinations.any { dest ->
            when (val route = dest.route) {
                is HomeRoute -> currentRoute is HomeRoute
                else -> route != null && route == currentRoute
            }
        }

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
            val selectedDest = appState.topLevelDestinations.find { dest ->
                when (val route = dest.route) {
                    is HomeRoute -> appState.navigationState.currentTopLevelKey is HomeRoute
                    else -> route != null && route == appState.navigationState.currentTopLevelKey
                }
            }

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
