package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileUiState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.ui.components.*

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditClick: () -> Unit = {},
    onDailyZonesClick: () -> Unit = {},
    onPreferenceClick: (String) -> Unit = {},
    onSettingsClick: (String) -> Unit = {},
    onThemeClick: (Boolean) -> Unit = {},
    onLanguageClick: (String) -> Unit = {},
    onUpdateSleepSchedule: (String, String) -> Unit = { _, _ -> },
    onUpdateSessionDuration: (Int) -> Unit = {},
    onUpdateTimezone: (String) -> Unit = {},
    onRetry: () -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AwanTheme.colors.background)
    ) {
        if (uiState.isLoading && uiState.profile == null) {
            ProfileShimmer()
        } else if (uiState.profile != null) {
            ProfileContent(
                profile = uiState.profile,
                uiState = uiState,
                onEditClick = onEditClick,
                onDailyZonesClick = onDailyZonesClick,
                onPreferenceClick = onPreferenceClick,
                onSettingsClick = onSettingsClick,
                onThemeClick = onThemeClick,
                onLanguageClick = onLanguageClick,
                onUpdateSleepSchedule = onUpdateSleepSchedule,
                onUpdateSessionDuration = onUpdateSessionDuration,
                onUpdateTimezone = onUpdateTimezone
            )
        } else if (uiState.errorMessage != null) {
            ProfileErrorState(
                errorMessage = uiState.errorMessage.asString(),
                onRetry = onRetry,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun ProfileErrorState(
    errorMessage: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AwanText(
            text = errorMessage,
            style = AwanTheme.styles.errorText,
        )
        AwanButton(
            onClick = onRetry,
            modifier = Modifier.wrapContentWidth()
        ) {
            AwanText(text = androidx.compose.ui.res.stringResource(ProfileR.string.profile_retry))
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
    onDailyZonesClick: () -> Unit,
    onPreferenceClick: (String) -> Unit,
    onSettingsClick: (String) -> Unit,
    onThemeClick: (Boolean) -> Unit,
    onLanguageClick: (String) -> Unit,
    onUpdateSleepSchedule: (String, String) -> Unit,
    onUpdateSessionDuration: (Int) -> Unit,
    onUpdateTimezone: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp)
    ) {
        ProfileHeaderCard(
            profile = profile,
            uiState = uiState,
            onEditClick = onEditClick
        )

        PreferencesCard(
            profile = profile,
            uiState = uiState,
            onDailyZonesClick = onDailyZonesClick,
            onPreferenceClick = onPreferenceClick,
            onUpdateSleepSchedule = onUpdateSleepSchedule,
            onUpdateSessionDuration = onUpdateSessionDuration,
            onUpdateTimezone = onUpdateTimezone
        )

        AppearanceCard(
            uiState = uiState,
            onThemeClick = onThemeClick,
            onLanguageClick = onLanguageClick
        )

        SettingsCard(
            onSettingsClick = onSettingsClick
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}
