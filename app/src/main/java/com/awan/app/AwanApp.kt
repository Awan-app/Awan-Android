package com.awan.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import com.awan.app.core.designsystem.AwanTheme
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.awan.app.core.designsystem.AwanBottomNavBar
import com.awan.app.core.designsystem.BottomNavItem
import com.awan.app.core.common.R as CommonR
import com.awan.app.core.designsystem.ObserveAsEvents
import kotlinx.coroutines.flow.Flow
import com.awan.app.core.domain.gamification.model.RewardEvent
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
import com.awan.feature.inventory.api.InventoryRoute
import com.awan.feature.inventory.impl.navigation.inventoryEntry
import com.awan.feature.marketplace.impl.navigation.marketplaceEntry
import com.awan.feature.auth.api.OtpRoute
import com.awan.feature.onboarding.api.OnboardingRoute
import com.awan.feature.onboarding.impl.navigation.onboardingEntry
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.api.McpInfoRoute
import com.awan.feature.profile.api.McpSettingsRoute
import com.awan.feature.profile.api.NotificationSettingsRoute
import com.awan.feature.profile.impl.navigation.profileEntry
import com.awan.feature.splash.api.SplashRoute
import com.awan.feature.splash.impl.navigation.splashEntry
import com.awan.feature.splash.impl.ui.SplashDestination

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
 */
@Composable
private fun NavigationState.rememberDecoratedEntries(
    entryProvider: (Route) -> NavEntry<Route>,
): List<NavEntry<Route>> {
    val decoratedStacks = subStacks.mapValues { (topLevelKey, stack) ->
        key(generation, topLevelKey) {
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
    rewardEvents: Flow<RewardEvent>,
    modifier: Modifier = Modifier,
    isOnline: Boolean = true,
    deepLinkSessionId: String? = null,
    deepLinkDate: String? = null,
    onDeepLinkHandled: () -> Unit = {},
) {
    val navigator = remember { Navigator(appState.navigationState) }
    var showAddTask by rememberSaveable { mutableStateOf(false) }
    var onSelectHomeDate by remember { mutableStateOf<((LocalDate) -> Unit)?>(null) }
    val currentRoute = appState.navigationState.currentKey
    val showOfflineBanner = !isOnline && currentRoute != SplashRoute

    // A notification can be tapped from any tab, so bring Home forward before it tries to open the
    // session. Guarded on the main shell being up: during splash or auth the user may still need to
    // log in, and the pending link survives until Home eventually composes.
    androidx.compose.runtime.LaunchedEffect(deepLinkSessionId, appState.navigationState.currentTopLevelKey) {
        if (deepLinkSessionId != null &&
            appState.navigationState.currentTopLevelKey in appState.navigationState.topLevelKeys
        ) {
            navigator.navigate(HomeRoute())
        }
    }

    val offlineExplanation = stringResource(R.string.app_offline_lock_explanation)
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(showAddTask, isOnline) {
        if (showAddTask && !isOnline) {
            android.widget.Toast.makeText(
                context,
                offlineExplanation,
                android.widget.Toast.LENGTH_LONG
            ).show()
            showAddTask = false
        }
    }

    // An expired token has to bounce the user out from wherever they are, so this stays at the
    // shell rather than on any one screen.
    val sessionExpiredMessage = stringResource(CommonR.string.error_unauthorized)
    ObserveAsEvents(sessionExpiredEvents) {
        android.widget.Toast.makeText(
            context,
            sessionExpiredMessage,
            android.widget.Toast.LENGTH_LONG,
        ).show()
        showAddTask = false
        navigator.replaceAll(LoginRoute)
    }

    if (showAddTask && isOnline) {
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
        Column(modifier = Modifier.fillMaxSize()) {
            AnimatedVisibility(
                visible = showOfflineBanner,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Surface(
                    color = AwanTheme.colors.streakSurface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WifiOff,
                            contentDescription = null,
                            tint = AwanTheme.colors.streakIcon,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.app_offline_banner_text),
                            color = AwanTheme.colors.streakIcon,
                            style = AwanTheme.typography.caption,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

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
            marketplaceEntry(
                onNavigateToHome = { navigator.navigate(HomeRoute()) }
            )
            homeEntry(
                onLogout = { navigator.replaceAll(LoginRoute) },
                onNavigateToCalendar = { navigator.navigate(com.awan.feature.calendar.api.CalendarRoute()) },
                onRegisterSelectDate = { callback -> onSelectHomeDate = callback },
                onNavigateToAddTask = { _, _ ->
                    showAddTask = true
                },
                deepLinkSessionId = deepLinkSessionId,
                deepLinkDate = deepLinkDate,
                onDeepLinkHandled = onDeepLinkHandled,
            )

            calendarEntry(
                onDateSelected = { date ->
                    onSelectHomeDate?.invoke(date)
                    navigator.goBack()
                },
                onBack = { navigator.goBack() },
            )
            chatEntry()
            goalsEntry()
            aiTasksEntry(onBack = { navigator.goBack() })
            inventoryEntry(onBack = { navigator.goBack() })
            profileEntry(
                onNavigateToDailyZones = { navigator.navigate(DailyZonesRoute) },
                onNavigateToEditRoutine = { templateId -> navigator.navigate(EditRoutineRoute(templateId)) },
                onLogout = { navigator.replaceAll(LoginRoute) },
                onBack = { navigator.goBack()},
                onNavigateToInventory = { navigator.navigate(InventoryRoute) },
                onNavigateToMcpSettings = { navigator.navigate(McpSettingsRoute) },
                onNavigateToMcpInfo = { navigator.navigate(McpInfoRoute) },
                onNavigateToNotificationSettings = { navigator.navigate(NotificationSettingsRoute) },
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
            modifier = Modifier.weight(1f)
        )
    }


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
                anchoredItemId = TopLevelDestination.PROFILE.name,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // Last child of the root Box: above every screen and the bottom bar, and in the same
        // coordinate space as the anchors it animates between — which a Dialog would not be.
        RewardOverlayHost(rewardEvents = rewardEvents)

        com.awan.app.core.designsystem.AwanTopToastHost()
    }
}
