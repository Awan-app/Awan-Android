package com.awan.feature.profile.impl.ui

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Timelapse
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanBackButton
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanChoiceRow
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.model.NotificationPreferences
import com.awan.feature.profile.impl.BuildConfig
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
                    // The two questions the reminder raises — how early, and how long "later" is —
                    // sit under the switch that decides whether it happens at all, and go away with
                    // it. A greyed-out row elsewhere on the screen is a setting the user has to
                    // find and then discover is inert.
                    AnimatedVisibility(visible = uiState.preferences.sessionRemindersEnabled) {
                        Column {
                            AwanChoiceRow(
                                icon = Icons.Default.Alarm,
                                title = stringResource(ProfileR.string.profile_notifications_reminder_lead),
                                options = NotificationPreferences.REMINDER_LEAD_CHOICES,
                                selected = uiState.preferences.reminderLeadMinutes,
                                enabled = uiState.systemNotificationsEnabled,
                                label = { stringResource(ProfileR.string.profile_notifications_minutes, it) },
                                showDivider = true,
                                onSelect = { onAction(NotificationSettingsAction.SetReminderLead(it)) },
                            )
                            // Snooze belongs to the reminder: it is a button on that notification
                            // and on no other.
                            AwanChoiceRow(
                                icon = Icons.Default.Snooze,
                                title = stringResource(ProfileR.string.profile_notifications_snooze_length),
                                options = NotificationPreferences.SNOOZE_CHOICES,
                                selected = uiState.preferences.snoozeMinutes,
                                enabled = uiState.systemNotificationsEnabled,
                                label = { minutes ->
                                    if (minutes == NotificationPreferences.SNOOZE_ASK) {
                                        stringResource(ProfileR.string.profile_notifications_snooze_ask)
                                    } else {
                                        stringResource(ProfileR.string.profile_notifications_minutes, minutes)
                                    }
                                },
                                showDivider = true,
                                onSelect = { onAction(NotificationSettingsAction.SetSnooze(it)) },
                            )
                        }
                    }
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
                        showDivider = true,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetSessionEnd(it)) },
                    )
                    SwitchRow(
                        icon = Icons.Default.QuestionAnswer,
                        title = stringResource(ProfileR.string.profile_notifications_follow_up),
                        checked = uiState.preferences.sessionFollowUpEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.sky,
                        showDivider = uiState.preferences.sessionFollowUpEnabled,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetSessionFollowUp(it)) },
                    )
                    AnimatedVisibility(visible = uiState.preferences.sessionFollowUpEnabled) {
                        AwanChoiceRow(
                            icon = Icons.Default.HourglassEmpty,
                            title = stringResource(ProfileR.string.profile_notifications_follow_up_delay),
                            options = NotificationPreferences.FOLLOW_UP_CHOICES,
                            selected = uiState.preferences.followUpDelayMinutes,
                            enabled = uiState.systemNotificationsEnabled,
                            label = { stringResource(ProfileR.string.profile_notifications_minutes, it) },
                            onSelect = { onAction(NotificationSettingsAction.SetFollowUpDelay(it)) },
                        )
                    }
                }
            }

            SectionTitle(stringResource(ProfileR.string.profile_notifications_section_nudges))
            AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
                Column {
                    SwitchRow(
                        icon = Icons.Default.LocalFireDepartment,
                        title = stringResource(ProfileR.string.profile_notifications_streak),
                        checked = uiState.preferences.streakReminderEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.zoneTangerine,
                        showDivider = true,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetStreakReminder(it)) },
                    )
                    SwitchRow(
                        icon = Icons.Default.WbSunny,
                        title = stringResource(ProfileR.string.profile_notifications_daily_brief),
                        checked = uiState.preferences.dailyBriefEnabled,
                        enabled = uiState.systemNotificationsEnabled,
                        iconColor = AwanTheme.colors.zoneTangerine,
                        onCheckedChange = { onAction(NotificationSettingsAction.SetDailyBrief(it)) },
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

            if (BuildConfig.DEBUG) {
                DebugNotificationsCard()
            }
        }
    }
}

/**
 * Fires each notification on the spot.
 *
 * Every one of these is time-gated — a lead time, a session's end, two hours before the user's day
 * ends — and the emulator images this is developed against will not let their clock be moved, so
 * without this the only way to see one is to wait for it.
 *
 * Sends the broadcast the debug-only receiver in `:core:notifications` listens for rather than
 * calling the poster, so nothing about notifications leaks into a ViewModel and the module needs no
 * new dependency. Same broadcast `adb shell am broadcast` sends.
 */
@Composable
private fun DebugNotificationsCard() {
    val context = LocalContext.current
    // ponytail: hardcoded English. This card is compiled out of release builds, and a translated
    // string in strings.xml would be shipped and translated for something no user will ever see.
    val kinds = listOf(
        "reminder" to "Session reminder",
        "live" to "Running session",
        "ended" to "Session ended",
        "followup" to "Follow-up",
        "streak" to "End of day nudge",
        "brief" to "Daily brief",
    )

    SectionTitle("DEBUG")
    AwanCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
        Column {
            kinds.forEachIndexed { index, (kind, title) ->
                PreferenceRow(
                    icon = Icons.Default.BugReport,
                    title = title,
                    showDivider = index < kinds.lastIndex,
                    iconColor = AwanTheme.colors.meta,
                    onClick = {
                        context.sendBroadcast(
                            Intent(DEBUG_NOTIFICATION_ACTION)
                                .setPackage(context.packageName)
                                .putExtra("kind", kind)
                        )
                    },
                )
            }
        }
    }
}

private const val DEBUG_NOTIFICATION_ACTION = "com.awan.app.DEBUG_NOTIFICATION"

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

