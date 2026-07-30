package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.presentation.ProfileAction
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.ui.components.EditPersonalInfoSheet
import com.awan.feature.profile.impl.ui.components.ProfileShimmer

@Composable
fun ProfileScreen(
    uiState: ProfileState,
    onAction: (ProfileAction) -> Unit,
    onDailyZonesClick: () -> Unit = {},
    onSettingsClick: (String) -> Unit = {},
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
                onAction = onAction,
                onEditClick = { showEditSheet = true },
                onDailyZonesClick = onDailyZonesClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = { showLogoutDialog = true }
            )

            if (showEditSheet) {
                EditPersonalInfoSheet(
                    initialFirstName = uiState.profile.firstName ?: "",
                    initialLastName = uiState.profile.lastName ?: "",
                    initialBirthDate = uiState.profile.birthDate ?: "",
                    onDismiss = { showEditSheet = false },
                    onSave = { first, last, birth ->
                        onAction(ProfileAction.UpdatePersonalInfo(first, last, birth))
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
                        onAction(ProfileAction.Logout)
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
                onRetry = { onAction(ProfileAction.Refresh) },
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
