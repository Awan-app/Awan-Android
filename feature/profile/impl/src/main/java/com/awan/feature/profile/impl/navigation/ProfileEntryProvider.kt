package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.DailyZonesRoute
import com.awan.feature.profile.api.DayDetailsRoute
import com.awan.feature.profile.api.EditProfileRoute
import com.awan.feature.profile.api.EditRoutineRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.api.RoutineDetailsRoute
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesViewModel
import com.awan.feature.profile.impl.presentation.DayDetailsAction
import com.awan.feature.profile.impl.presentation.DayDetailsViewModel
import com.awan.feature.profile.impl.presentation.EditProfileEvent
import com.awan.feature.profile.impl.presentation.EditProfileViewModel
import com.awan.feature.profile.impl.presentation.EditRoutineAction
import com.awan.feature.profile.impl.presentation.EditRoutineEvent
import com.awan.feature.profile.impl.presentation.EditRoutineViewModel
import com.awan.feature.profile.impl.presentation.ProfileEvent
import com.awan.feature.profile.impl.presentation.ProfileAction
import com.awan.feature.profile.impl.presentation.ProfileEvent
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.presentation.RoutineDetailsAction
import com.awan.feature.profile.impl.presentation.RoutineDetailsEvent
import com.awan.feature.profile.impl.presentation.RoutineDetailsViewModel
import com.awan.feature.profile.impl.ui.EditProfileScreen
import com.awan.feature.profile.impl.ui.EditRoutineScreen
import com.awan.feature.profile.impl.ui.ProfileScreen
import com.awan.feature.profile.impl.ui.dailyzones.DailyZonesScreen
import com.awan.feature.profile.impl.ui.daydetails.DayDetailsScreen
import com.awan.feature.profile.impl.ui.routinedetails.RoutineDetailsScreen
import kotlinx.coroutines.flow.collectLatest

fun EntryProviderScope<Route>.profileEntry(
    onNavigateToEditProfile: () -> Unit,
    onNavigateToDailyZones: () -> Unit,
    onNavigateToRoutineDetails: (String) -> Unit,
    onNavigateToEditRoutine: (String?) -> Unit,
    onNavigateToDayDetails: (String) -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onEditClick = onNavigateToEditProfile,
            onDailyZonesClick = onNavigateToDailyZones,
            onLogout = onLogout
        )
    }

    entry<EditProfileRoute> {
        EditProfileRouteScreen(
            onBack = onBack
        )
    }

    entry<DailyZonesRoute> {
        DailyZonesRouteScreen(
            onRoutineClick = onNavigateToRoutineDetails,
            onCreateRoutineClick = { onNavigateToEditRoutine(null) },
            onDayClick = onNavigateToDayDetails,
            onBack = onBack
        )
    }

    entry<RoutineDetailsRoute> { route ->
        RoutineDetailsRouteScreen(
            templateId = route.templateId,
            onEditRoutine = { onNavigateToEditRoutine(route.templateId) },
            onBack = onBack
        )
    }

    entry<EditRoutineRoute> { route ->
        EditRoutineRouteScreen(
            templateId = route.templateId,
            onBack = onBack
        )
    }

    entry<DayDetailsRoute> { route ->
        DayDetailsRouteScreen(
            date = route.date,
            onBack = onBack
        )
    }
}

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onEditClick: () -> Unit,
    onDailyZonesClick: () -> Unit,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                ProfileEvent.LogoutSuccess -> onLogout()
            }
        }
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                ProfileEvent.LogoutSuccess -> onLogout()
            }
        }
    }

    ProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onEditClick = onEditClick,
        onDailyZonesClick = onDailyZonesClick,
        onDailyZonesClick = { },
        onSettingsClick = { _ -> },
    )
}

@Composable
fun EditProfileRouteScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                EditProfileEvent.SaveSuccess -> onBack()
            }
        }
    }

    EditProfileScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack
    )
}

@Composable
fun DailyZonesRouteScreen(
    viewModel: DailyZonesViewModel = hiltViewModel(),
    onRoutineClick: (String) -> Unit,
    onCreateRoutineClick: () -> Unit,
    onDayClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.onAction(DailyZonesAction.LoadData)
    }

    DailyZonesScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToDayDetails = onDayClick,
        onNavigateToRoutineDetails = onRoutineClick,
        onCreateRoutineClick = onCreateRoutineClick,
        onBackClick = onBack,
    )
}

@Composable
fun RoutineDetailsRouteScreen(
    templateId: String,
    viewModel: RoutineDetailsViewModel = hiltViewModel(),
    onEditRoutine: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(templateId) {
        viewModel.onAction(RoutineDetailsAction.LoadTemplate(templateId))
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                RoutineDetailsEvent.DeleteSuccess -> onBack()
            }
        }
    }

    RoutineDetailsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onEditClick = onEditRoutine,
        onBackClick = onBack
    )
}

@Composable
fun EditRoutineRouteScreen(
    templateId: String?,
    viewModel: EditRoutineViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(templateId) {
        viewModel.onAction(EditRoutineAction.LoadTemplate(templateId))
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                EditRoutineEvent.SaveSuccess -> onBack()
            }
        }
    }

    EditRoutineScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack
    )
}

@Composable
fun DayDetailsRouteScreen(
    date: String,
    viewModel: DayDetailsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(date) {
        viewModel.onAction(DayDetailsAction.LoadDayDetails(date))
    }

    DayDetailsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack
        onThemeClick = { useDarkTheme ->
            viewModel.onAction(ProfileAction.SetTheme(useDarkTheme))
        },
        onLanguageClick = { languageCode ->
            viewModel.onAction(ProfileAction.SetLanguage(languageCode))
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        },
        onUpdateSleepSchedule = { wakeup, sleep ->
            viewModel.onAction(ProfileAction.UpdateSleepSchedule(wakeup, sleep))
        },
        onUpdateSessionDuration = { duration ->
            viewModel.onAction(ProfileAction.UpdateSessionDuration(duration))
        },
        onUpdateTimezone = { timezone ->
            viewModel.onAction(ProfileAction.UpdateTimezone(timezone))
        },
        onUpdatePersonalInfo = { first, last, birth ->
            viewModel.onAction(ProfileAction.UpdatePersonalInfo(first, last, birth))
        },
        onLogout = { viewModel.onAction(ProfileAction.Logout) },
        onRetry = { viewModel.onAction(ProfileAction.Refresh) }
    )
}
