package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileAction
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.ui.components.ProfileHeaderCard
import com.awan.feature.profile.impl.ui.components.PreferencesCard
import com.awan.feature.profile.impl.ui.components.AppearanceCard
import com.awan.feature.profile.impl.ui.components.SettingsCard

@Composable
fun ProfileContent(
    profile: Profile,
    uiState: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onEditClick: () -> Unit,
    onPictureClick: () -> Unit,
    onDailyZonesClick: () -> Unit,
    onSettingsClick: (String) -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AwanText(
            text = stringResource(ProfileR.string.profile_title),
            style = AwanTheme.styles.titleText,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        ProfileHeaderCard(
            profile = profile,
            uiState = uiState,
            onEditClick = onEditClick,
            onPictureClick = onPictureClick
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
            onLogoutClick = onLogoutClick
        )

        Spacer(modifier = Modifier.height(100.dp))
    }
}
