package com.awan.feature.profile.impl.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.ProfileScreen

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
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreen(
        uiState = uiState,
        onDailyZonesClick = { },
        onSettingsClick = { _ -> },
        onThemeClick = viewModel::setTheme,
        onLanguageClick = { languageCode ->
            viewModel.setLanguage(languageCode)
            val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocale)
        },
        onUpdateSleepSchedule = { wakeup, sleep ->
            viewModel.updateSleepSchedule(wakeup, sleep)
        },
        onUpdateSessionDuration = viewModel::updateSessionDuration,
        onUpdateTimezone = viewModel::updateTimezone,
        onUpdatePersonalInfo = viewModel::updatePersonalInfo,
        onLogout = { viewModel.logout(onLogout) },
        onRetry = viewModel::refresh
    )
}
