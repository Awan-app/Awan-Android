package com.awan.feature.profile.impl.navigation

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.os.LocaleListCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.profile.api.EditProfileRoute
import com.awan.feature.profile.api.ProfileRoute
import com.awan.feature.profile.impl.presentation.EditProfileViewModel
import com.awan.feature.profile.impl.presentation.ProfileViewModel
import com.awan.feature.profile.impl.ui.EditProfileScreen
import com.awan.feature.profile.impl.ui.ProfileScreen
import com.awan.feature.profile.impl.ui.components.LanguageSelectionDialog
import com.awan.feature.profile.impl.ui.components.TimezoneSelectionDialog

fun EntryProviderScope<Route>.profileEntry(
    onNavigateToEditProfile: () -> Unit,
    onBack: () -> Unit,
) {
    entry<ProfileRoute> {
        ProfileRouteScreen(
            onEditClick = onNavigateToEditProfile
        )
    }

    entry<EditProfileRoute> {
        EditProfileRouteScreen(
            onBack = onBack
        )
    }
}

@Composable
fun ProfileRouteScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onEditClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    ProfileScreen(
        uiState = uiState,
        onEditClick = onEditClick,
        onDailyZonesClick = { },
        onPreferenceClick = { _ -> },
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
        onRetry = viewModel::refresh
    )
}

@Composable
fun EditProfileRouteScreen(
    viewModel: EditProfileViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    
    EditProfileScreen(
        uiState = uiState,
        onFirstNameChange = viewModel::onFirstNameChange,
        onLastNameChange = viewModel::onLastNameChange,
        onBirthDateChange = viewModel::onBirthDateChange,
        onUpdateSleepSchedule = viewModel::updateSleepSchedule,
        onUpdateSessionDuration = viewModel::updateSessionDuration,
        onUpdateSchedulingType = viewModel::updateSchedulingType,
        onUpdateTimezone = viewModel::updateTimezone,
        onSaveClick = viewModel::saveProfile,
        onBackClick = onBack
    )
}
