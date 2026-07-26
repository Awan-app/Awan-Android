package com.awan.feature.profile.impl.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.impl.presentation.ProfileAction
import com.awan.feature.profile.impl.presentation.ProfileEvent
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.ProfileScreen
import kotlinx.coroutines.flow.collectLatest

fun EntryProviderScope<Route>.profileEntry(
    onLogout: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onLogout = onLogout
        )
    }
}

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
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

    ProfileScreen(
        uiState = uiState,
        onDailyZonesClick = { },
        onSettingsClick = { _ -> },
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
