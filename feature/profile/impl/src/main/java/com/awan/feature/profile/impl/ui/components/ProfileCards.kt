package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileUiState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.app.core.designsystem.R as DesignR

@Composable
fun ProfileHeaderCard(
    profile: Profile,
    uiState: ProfileUiState,
    onEditClick: () -> Unit,
) {
    val isDark = uiState.useDarkTheme

    val mascotBgColor by animateColorAsState(
        targetValue = if (isDark) Color(0xFF1B2B48) else Color(0xFFFFD93D),
        animationSpec = tween(1000, easing = LinearOutSlowInEasing),
        label = "mascotBg"
    )

    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(mascotBgColor)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Celestial Body (Sun/Moon) peek-a-boo
                val celestialColor by animateColorAsState(
                    targetValue = if (isDark) Color(0xFFE0E0E0) else Color(0xFFFFB74D),
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                    label = "celestialColor"
                )

                val celestialRotation by animateFloatAsState(
                    targetValue = if (isDark) -15f else 0f,
                    animationSpec = tween(1000, easing = LinearOutSlowInEasing),
                    label = "celestialRotation"
                )

                Icon(
                    imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = celestialColor.copy(alpha = 0.7f),
                    modifier = Modifier
                        .size(32.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .graphicsLayer {
                            rotationZ = celestialRotation
                        }
                )

                AwanMascot(
                    expression = if (isDark) MascotExpression.Idle else MascotExpression.Greet,
                    blinkEnabled = !isDark,
                    width = 64.dp,
                    modifier = Modifier.graphicsLayer {
                        val scale = if (!isDark) 1.1f else 1.0f
                        scaleX = scale
                        scaleY = scale
                    }
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = "${profile.firstName} ${profile.lastName}",
                    style = AwanTheme.styles.titleText.copy(
                        textStyle = AwanTheme.typography.title.copy(fontSize = 18.sp)
                    )
                )
                AwanText(
                    text = profile.email ?: "",
                    style = AwanTheme.styles.bodySecondaryText.copy(
                        textStyle = AwanTheme.typography.body.copy(fontSize = 12.sp)
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            IconButton(
                onClick = onEditClick,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(AwanTheme.colors.sky.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(ProfileR.string.profile_edit),
                    tint = AwanTheme.colors.sky,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(
                icon = Icons.Default.Whatshot,
                value = profile.streak.toString(),
                label = stringResource(ProfileR.string.profile_streak),
                iconTint = AwanTheme.colors.zoneTangerine
            )
            StatItem(
                icon = Icons.Default.Star,
                value = profile.maxStreak.toString(),
                label = stringResource(ProfileR.string.profile_max_streak),
                iconTint = AwanTheme.colors.zoneSun
            )
            StatItem(
                icon = Icons.Default.Diamond,
                value = profile.points.toString(),
                label = stringResource(ProfileR.string.profile_total_points),
                iconTint = AwanTheme.colors.sky
            )
        }
    }
}

@Composable
fun PreferencesCard(
    profile: Profile,
    uiState: ProfileUiState,
    onDailyZonesClick: () -> Unit,
    onPreferenceClick: (String) -> Unit,
    onUpdateSleepSchedule: (String, String) -> Unit,
    onUpdateSessionDuration: (Int) -> Unit,
    onUpdateTimezone: (String) -> Unit,
) {
    var expandedItem by remember { mutableStateOf<String?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(ProfileR.string.profile_section_preferences))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            DailyZonesItem(
                icon = Icons.Default.DashboardCustomize,
                title = stringResource(ProfileR.string.profile_daily_zones),
                subtitle = stringResource(ProfileR.string.profile_daily_zones_subtitle),
                onClick = onDailyZonesClick,
                showDivider = true
            )
            
            ExpandableSessionDurationItem(
                duration = profile.preferences?.preferredSessionDuration ?: 60,
                isExpanded = expandedItem == "session_time",
                onExpandClick = { expandedItem = if (expandedItem == "session_time") null else "session_time" },
                onSaveClick = { duration ->
                    onUpdateSessionDuration(duration)
                    expandedItem = null
                },
                onCancelClick = { expandedItem = null },
                isLoading = uiState.isUpdatingField,
                showDivider = true
            )

            ExpandableTimezoneItem(
                currentSelection = profile.preferences?.timezone ?: "UTC",
                isExpanded = expandedItem == "timezone",
                onExpandClick = { expandedItem = if (expandedItem == "timezone") null else "timezone" },
                onTimezoneSelected = { tz ->
                    onUpdateTimezone(tz)
                    // We don't collapse immediately to show loading state if needed,
                    // but usually timezone is a quick tap. User can collapse by clicking header.
                },
                isLoading = uiState.isUpdatingField,
                showDivider = true
            )

            ExpandableTimePickerItem(
                icon = Icons.Default.WbSunny,
                title = stringResource(ProfileR.string.profile_wakeup_time),
                hour = parseHour(profile.preferences?.wakeupTime),
                minute = parseMinute(profile.preferences?.wakeupTime),
                isExpanded = expandedItem == "wakeup",
                onExpandClick = { expandedItem = if (expandedItem == "wakeup") null else "wakeup" },
                onSaveClick = { h, m ->
                    onUpdateSleepSchedule(formatToApiTime(h, m), profile.preferences?.sleepTime ?: "23:00:00")
                    expandedItem = null
                },
                onCancelClick = { expandedItem = null },
                isLoading = uiState.isUpdatingField,
                showDivider = true
            )

            ExpandableTimePickerItem(
                icon = Icons.Default.NightsStay,
                title = stringResource(ProfileR.string.profile_sleep_time),
                hour = parseHour(profile.preferences?.sleepTime),
                minute = parseMinute(profile.preferences?.sleepTime),
                isExpanded = expandedItem == "sleep",
                onExpandClick = { expandedItem = if (expandedItem == "sleep") null else "sleep" },
                onSaveClick = { h, m ->
                    onUpdateSleepSchedule(profile.preferences?.wakeupTime ?: "07:30:00", formatToApiTime(h, m))
                    expandedItem = null
                },
                onCancelClick = { expandedItem = null },
                isLoading = uiState.isUpdatingField,
                showDivider = false
            )
        }
    }
}

@Composable
fun AppearanceCard(
    uiState: ProfileUiState,
    onThemeClick: (Boolean) -> Unit,
    onLanguageClick: (String) -> Unit,
) {
    var isLanguageExpanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(ProfileR.string.profile_section_appearance))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            ExpandableLanguageItem(
                currentLanguage = uiState.language,
                isExpanded = isLanguageExpanded,
                onExpandClick = { isLanguageExpanded = !isLanguageExpanded },
                onLanguageSelected = onLanguageClick,
                showDivider = true
            )

            PreferenceRow(
                icon = Icons.Default.Contrast,
                title = stringResource(ProfileR.string.profile_theme),
                onClick = null,
                iconColor = AwanTheme.colors.zoneViolet
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(end = 16.dp)
                ) {
                    ThemeOption(
                        label = stringResource(ProfileR.string.profile_theme_light),
                        isSelected = !uiState.useDarkTheme,
                        onClick = { onThemeClick(false) }
                    )
                    ThemeOption(
                        label = stringResource(ProfileR.string.profile_theme_dark),
                        isSelected = uiState.useDarkTheme,
                        onClick = { onThemeClick(true) }
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    onSettingsClick: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(stringResource(ProfileR.string.profile_section_settings))
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(
                icon = Icons.Default.NotificationsActive,
                title = stringResource(ProfileR.string.profile_notifications),
                value = "Enabled",
                onClick = { onSettingsClick("notifications") },
                showDivider = true,
                iconColor = AwanTheme.colors.zoneTangerine
            )
            PreferenceRow(
                icon = Icons.AutoMirrored.Filled.Help,
                title = stringResource(ProfileR.string.profile_help_center),
                onClick = { onSettingsClick("help") },
                iconColor = AwanTheme.colors.meta
            )
        }
    }
}

@Composable
fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        AwanText(
            text = value,
            style = AwanTheme.styles.headingText.copy(
                textStyle = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
        )
        AwanText(
            text = label,
            style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.meta)
        )
    }
}

private fun parseHour(time: String?): Int {
    if (time == null) return 7
    return try {
        time.split(":")[0].toInt()
    } catch (_: Exception) {
        7
    }
}

private fun parseMinute(time: String?): Int {
    if (time == null) return 30
    return try {
        time.split(":")[1].toInt()
    } catch (_: Exception) {
        30
    }
}

private fun formatToApiTime(hour: Int, minute: Int): String {
    return String.format(java.util.Locale.US, "%02d:%02d:00", hour, minute)
}
