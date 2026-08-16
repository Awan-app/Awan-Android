package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R as ProfileR

/**
 * Shown when the OS notification permission is off.
 *
 * Until this existed the permission was only ever asked for during onboarding, so a user who
 * declined there had no way back into it from inside the app.
 */
@Composable
fun SystemNotificationsDisabledCard(
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AwanCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = AwanTheme.spacing.sm),
        contentPadding = PaddingValues(AwanTheme.spacing.md),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)) {
            AwanText(
                text = stringResource(ProfileR.string.profile_notifications_blocked_title),
                style = AwanTheme.styles.headingText,
            )
            AwanText(
                text = stringResource(ProfileR.string.profile_notifications_blocked_body),
                style = AwanTheme.styles.bodySecondaryText,
            )
            AwanButton(
                onClick = onOpenSystemSettings,
                variant = AwanButtonVariant.Primary,
            ) {
                AwanText(stringResource(ProfileR.string.profile_notifications_blocked_action))
            }
        }
    }
}
