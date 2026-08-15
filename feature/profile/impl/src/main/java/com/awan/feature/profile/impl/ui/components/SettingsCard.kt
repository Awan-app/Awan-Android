package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun SettingsCard(
    onSettingsClick: (String) -> Unit,
    onInventoryClick: () -> Unit,
    onLogoutClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(ProfileR.string.profile_section_settings))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            // No `value`: PreferenceRow swaps the chevron for an ExpandMore whenever both a value
            // and an onClick are given, which made this row look expandable rather than navigable.
            PreferenceRow(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(ProfileR.string.profile_notifications),
                onClick = { onSettingsClick("notifications") },
                showDivider = true,
                iconColor = AwanTheme.colors.zoneTangerine
            )
            PreferenceRow(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(ProfileR.string.profile_help_center),
                onClick = { onSettingsClick("help") },
                showDivider = true,
                iconColor = AwanTheme.colors.meta
            )
            PreferenceRow(
                icon = Icons.Default.AutoAwesome,
                title = stringResource(ProfileR.string.profile_inventory),
                onClick = onInventoryClick,
                showDivider = true,
                iconColor = AwanTheme.colors.sky
            )
            PreferenceRow(
                icon = Icons.Default.VpnKey,
                title = stringResource(ProfileR.string.profile_mcp_title),
                onClick = { onSettingsClick("mcp") },
                showDivider = true,
                iconColor = AwanTheme.colors.sky
            )
            PreferenceRow(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = stringResource(ProfileR.string.profile_logout),
                onClick = onLogoutClick,
                iconColor = AwanTheme.colors.destructive,
                titleColor = AwanTheme.colors.destructive
            )
        }
    }
}
