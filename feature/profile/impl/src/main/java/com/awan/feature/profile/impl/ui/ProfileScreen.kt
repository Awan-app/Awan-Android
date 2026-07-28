package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileUiState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.ui.components.EditPersonalInfoSheet
import com.awan.feature.profile.impl.ui.components.ProfileHeaderCard
import com.awan.feature.profile.impl.ui.components.PreferencesCard
import com.awan.feature.profile.impl.ui.components.AppearanceCard
import com.awan.feature.profile.impl.ui.components.SettingsCard
import com.awan.feature.profile.impl.ui.components.ProfileShimmer

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onDailyZonesClick: () -> Unit = {},
    onSettingsClick: (String) -> Unit = {},
    onThemeClick: (Boolean) -> Unit = {},
    onLanguageClick: (String) -> Unit = {},
    onUpdateSleepSchedule: (String, String) -> Unit = { _, _ -> },
    onUpdateSessionDuration: (Int) -> Unit = {},
    onUpdateTimezone: (String) -> Unit = {},
    onUpdatePersonalInfo: (String, String, String) -> Unit = { _, _, _ -> },
    onLogout: () -> Unit = {},
    onRetry: () -> Unit = {},
) {
    var showEditSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

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
                onEditClick = { showEditSheet = true },
                onDailyZonesClick = onDailyZonesClick,
                onSettingsClick = onSettingsClick,
                onThemeClick = onThemeClick,
                onLanguageClick = onLanguageClick,
                onUpdateSleepSchedule = onUpdateSleepSchedule,
                onUpdateSessionDuration = onUpdateSessionDuration,
                onUpdateTimezone = onUpdateTimezone,
                onLogout = { showLogoutDialog = true }
            )

            if (showEditSheet) {
                EditPersonalInfoSheet(
                    initialFirstName = uiState.profile.firstName ?: "",
                    initialLastName = uiState.profile.lastName ?: "",
                    initialBirthDate = uiState.profile.birthDate ?: "",
                    onDismiss = { showEditSheet = false },
                    onSave = { first, last, birth ->
                        onUpdatePersonalInfo(first, last, birth)
                        showEditSheet = false
                    },
                    isLoading = uiState.isUpdatingField
                )
            }

            if (showLogoutDialog) {
                AwanDialog(
                    title = stringResource(ProfileR.string.profile_logout_confirm_title),
                    body = stringResource(ProfileR.string.profile_logout_confirm_subtitle),
                    icon = {
                        AwanMascot(
                            expression = MascotExpression.Curious,
                            width = AwanTheme.spacing.xxl * 3,
                            blinkEnabled = true
                        )
                    },
                    primaryLabel = stringResource(ProfileR.string.profile_logout),
                    primaryVariant = AwanButtonVariant.Destructive,
                    onPrimary = {
                        onLogout()
                        showLogoutDialog = false
                    },
                    secondaryLabel = stringResource(ProfileR.string.profile_cancel),
                    onSecondary = { showLogoutDialog = false },
                    onDismiss = { showLogoutDialog = false }
                )
            }
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
    onSettingsClick: (String) -> Unit,
    onThemeClick: (Boolean) -> Unit,
    onLanguageClick: (String) -> Unit,
    onUpdateSleepSchedule: (String, String) -> Unit,
    onUpdateSessionDuration: (Int) -> Unit,
    onUpdateTimezone: (String) -> Unit,
    onLogout: () -> Unit,
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
            onSettingsClick = onSettingsClick,
            onLogoutClick = onLogout
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}
