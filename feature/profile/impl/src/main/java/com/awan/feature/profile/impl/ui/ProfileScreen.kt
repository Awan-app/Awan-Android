package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileAction
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.ui.components.*

@Composable
fun ProfileScreen(
    uiState: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onEditClick: () -> Unit = {},
    onDailyZonesClick: () -> Unit = {},
    onSettingsClick: (String) -> Unit = {},
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
                onAction = onAction,
                onEditClick = onEditClick,
                onDailyZonesClick = onDailyZonesClick,
                onSettingsClick = onSettingsClick,
            )
        } else if (uiState.errorMessage != null) {
            ProfileErrorState(
                errorMessage = uiState.errorMessage.asString(),
                onRetry = { onAction(ProfileAction.Refresh) },
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
            AwanText(text = stringResource(ProfileR.string.profile_retry))
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    uiState: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onEditClick: () -> Unit,
    onDailyZonesClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AwanText(
            text = stringResource(ProfileR.string.profile_title),
            style = AwanTheme.styles.headingText.copy(
                textStyle = AwanTheme.typography.heading.copy(
                    fontSize = 32.sp,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold
                )
            ),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ProfileHeaderCard(
            profile = profile,
            uiState = uiState,
            onEditClick = onEditClick
        )

        PreferencesCard(
            profile = profile,
            uiState = uiState,
            onDailyZonesClick = onDailyZonesClick,
            onUpdateSleepSchedule = { wake, sleep -> onAction(ProfileAction.UpdateSleepSchedule(wake, sleep)) },
            onUpdateSessionDuration = { onAction(ProfileAction.UpdateSessionDuration(it)) },
            onUpdateTimezone = { onAction(ProfileAction.UpdateTimezone(it)) }
        )

        AppearanceCard(
            uiState = uiState,
            onThemeClick = { onAction(ProfileAction.SetTheme(it)) },
            onLanguageClick = { onAction(ProfileAction.SetLanguage(it)) }
        )

        SettingsCard(
            onSettingsClick = onSettingsClick,
            onLogoutClick = { onAction(ProfileAction.Logout) }
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
