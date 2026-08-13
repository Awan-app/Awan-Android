package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChoiceRow
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.NotificationPreferences
import com.awan.feature.profile.impl.presentation.NotificationSettingsAction
import com.awan.feature.profile.impl.presentation.NotificationSettingsState
import com.awan.feature.profile.impl.ui.components.PreferenceRow
import com.awan.feature.profile.impl.ui.components.SectionTitle
import com.awan.feature.profile.impl.ui.components.SystemNotificationsDisabledCard
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun NotificationSettingsScreen(
    uiState: NotificationSettingsState,
    onAction: (NotificationSettingsAction) -> Unit,
    onBackClick: () -> Unit,
    onOpenSystemSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AwanTheme.spacing.md, vertical = AwanTheme.spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AwanBackButton(onClick = onBackClick)
                AwanText(
                    text = stringResource(ProfileR.string.profile_notifications_title),
                    style = AwanTheme.styles.titleText,
                )
                // Balances the back button so the title stays centred.
                Box(modifier = Modifier.padding(horizontal = 20.dp))
            }
        },
        containerColor = AwanTheme.colors.background,
        modifier = modifier,
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AwanTheme.spacing.lg, vertical = AwanTheme.spacing.md),
        ) {
            if (!uiState.systemNotificationsEnabled) {
                SystemNotificationsDisabledCard(onOpenSystemSettings = onOpenSystemSettings)
            }

            SectionTitle(stringResource(ProfileR.string.profile_notifications_section_sessions))
            AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    SwitchRow(
                        icon = Icons.Default.NotificationsActive,
                        title = stringResource(ProfileR.string.profile_notifications_reminders),
                        checked = uiState.preferences.sessionRemindersEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.zoneTangerine,
                        showDivider = true,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetSessionReminders(it)) },
                    )
                    SwitchRow(
                        icon = Icons.Default.Timelapse,
                        title = stringResource(ProfileR.string.profile_notifications_live_activity),
                        checked = uiState.preferences.sessionLiveActivityEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.sky,
                        showDivider = true,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetLiveActivity(it)) },
                    )
                    SwitchRow(
                        icon = Icons.Default.DoneAll,
                        title = stringResource(ProfileR.string.profile_notifications_session_end),
                        checked = uiState.preferences.sessionEndEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.sky,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetSessionEnd(it)) },
                    )
                }
            }

            SectionTitle(stringResource(ProfileR.string.profile_notifications_section_timing))
            AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    AwanChoiceRow(
                        icon = Icons.Default.Alarm,
                        title = stringResource(ProfileR.string.profile_notifications_reminder_lead),
                        options = NotificationPreferences.REMINDER_LEAD_CHOICES,
                        selected = uiState.preferences.reminderLeadMinutes,
                        enabled = uiState.systemNotificationsEnabled &&
                            uiState.preferences.sessionRemindersEnabled,
                        label = { stringResource(ProfileR.string.profile_notifications_minutes, it) },
                        showDivider = true,
                        onSelect = { onAction(NotificationSettingsAction.SetReminderLead(it)) },
                    )
                    AwanChoiceRow(
                        icon = Icons.Default.Snooze,
                        title = stringResource(ProfileR.string.profile_notifications_snooze_length),
                        options = NotificationPreferences.SNOOZE_CHOICES,
                        selected = uiState.preferences.snoozeMinutes,
                        enabled = uiState.systemNotificationsEnabled,
                        label = { stringResource(ProfileR.string.profile_notifications_minutes, it) },
                        onSelect = { onAction(NotificationSettingsAction.SetSnooze(it)) },
                    )
                }
            }

            SectionTitle(stringResource(ProfileR.string.profile_notifications_section_other))
            AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    SwitchRow(
                        icon = Icons.Default.CardGiftcard,
                        title = stringResource(ProfileR.string.profile_notifications_rewards),
                        checked = uiState.preferences.rewardsEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.zoneTangerine,
                        showDivider = true,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetRewards(it)) },
                    )
                    // Per-channel importance, sound and the Live Update opt-out live in the OS, not
                    // here; this is the shortcut rather than a second copy of those controls.
                    PreferenceRow(
                        icon = Icons.Default.Settings,
                        title = stringResource(ProfileR.string.profile_notifications_system_settings),
                        onClick = onOpenSystemSettings,
                        iconColor = AwanTheme.colors.meta,
                    )
                }
            }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    checked: Boolean,
    enabled: Boolean,
    iconColor: androidx.compose.ui.graphics.Color,
    onCheckedChange: (Boolean) -> Unit,
    showDivider: Boolean = false,
) {
    PreferenceRow(
        icon = icon,
        title = title,
        // The row is the control; a switch is a small target to ask someone to hit.
        onClick = if (enabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
        showDivider = showDivider,
        iconColor = iconColor,
    ) {
        Switch(
            checked = checked,
            enabled = enabled,
            // Null, not a second handler: the switch draws the state and lets the press fall through
            // to the row, so a tap on the thumb toggles once and the row stays a single control for
            // accessibility rather than two overlapping ones.
            onCheckedChange = null,
            modifier = Modifier.padding(end = 12.dp),
        )
    }
}

