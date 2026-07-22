package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.presentation.ProfileUiState
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.app.core.designsystem.R as DesignR

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    onEditClick: () -> Unit = {},
    onDailyZonesClick: () -> Unit = {},
    onPreferenceClick: (String) -> Unit = {},
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
                onEditClick = onEditClick,
                onDailyZonesClick = onDailyZonesClick,
                onPreferenceClick = onPreferenceClick,
                onSettingsClick = onSettingsClick
            )
        }
    }
}

@Composable
private fun ProfileContent(
    profile: Profile,
    onEditClick: () -> Unit,
    onDailyZonesClick: () -> Unit,
    onPreferenceClick: (String) -> Unit,
    onSettingsClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Profile Card
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = DesignR.drawable.awan_mascot_idle),
                    contentDescription = null,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.background)
                )
                Column {
                    AwanText(
                        text = "${profile.firstName} ${profile.lastName}",
                        style = AwanTheme.styles.titleText
                    )
                    AwanText(
                        text = profile.email ?: "",
                        style = AwanTheme.styles.captionText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AwanTheme.colors.meta,
                            modifier = Modifier.size(12.dp)
                        )
                        AwanText(
                            text = profile.birthDate ?: "",
                            style = AwanTheme.styles.captionText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Whatshot,
                    value = "${profile.streak} days",
                    label = "Streak",
                    iconTint = AwanTheme.colors.zoneTangerine
                )
                StatItem(
                    icon = Icons.Default.Star,
                    value = "${profile.maxStreak} days",
                    label = "Max Streak",
                    iconTint = AwanTheme.colors.zoneSun
                )
                StatItem(
                    icon = Icons.Default.Diamond,
                    value = profile.points.toString(),
                    label = "Total Points",
                    iconTint = AwanTheme.colors.sky
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AwanButton(
                onClick = onEditClick,
                modifier = Modifier.fillMaxWidth(),
                variant = AwanButtonVariant.Secondary,
                icon = Icons.Default.Edit
            ) {
                AwanText(text = "Edit")
            }
        }

        // Preferences Section
        SectionTitle("PREFERENCES")
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceItem(
                icon = Icons.Default.CalendarToday,
                title = "Daily zones",
                subtitle = "Set the rhythm for each day",
                onClick = onDailyZonesClick,
                showDivider = true,
                showArrow = true
            )
            PreferenceRow(
                icon = Icons.Default.EventNote,
                title = "Scheduling",
                value = profile.preferences?.schedulingType ?: "BALANCED",
                onClick = { onPreferenceClick("scheduling") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.AccessTime,
                title = "Session time",
                value = "${profile.preferences?.preferredSessionDuration ?: 60} min",
                onClick = { onPreferenceClick("session_time") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.Public,
                title = "Time zone",
                value = profile.preferences?.timezone ?: "UTC",
                onClick = { onPreferenceClick("timezone") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.Bedtime,
                title = "Sleep schedule",
                value = "${profile.preferences?.wakeupTime?.take(5) ?: "07:00"} - ${profile.preferences?.sleepTime?.take(5) ?: "23:00"}",
                onClick = { onPreferenceClick("sleep_schedule") }
            )
        }

        // Language & Appearance
        SectionTitle("LANGUAGE & APPEARANCE")
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(
                icon = Icons.Default.Translate,
                title = "Language",
                value = "English",
                onClick = { onPreferenceClick("language") },
                showDivider = true
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AwanTheme.colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AwanText(text = "Theme", style = AwanTheme.styles.bodyText)
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeOption("Light", isSelected = true)
                    ThemeOption("System", isSelected = false)
                    ThemeOption("Dark", isSelected = false)
                }
            }
        }

        // Settings Section
        SectionTitle("Settings")
        AwanCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp)
        ) {
            PreferenceRow(
                icon = Icons.Default.Notifications,
                title = "Notifications",
                value = "On",
                onClick = { onSettingsClick("notifications") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.Info,
                title = "Help Center",
                onClick = { onSettingsClick("help") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.Help,
                title = "How to Earn Points",
                onClick = { onSettingsClick("points_info") },
                showDivider = true
            )
            PreferenceRow(
                icon = Icons.Default.ThumbUp,
                title = "Rate Awan",
                onClick = { onSettingsClick("rate") }
            )
        }
        
        Spacer(modifier = Modifier.height(80.dp)) // Nav bar padding
    }
}

@Composable
private fun SectionTitle(title: String) {
    AwanText(
        text = title,
        style = AwanTheme.styles.captionText,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun StatItem(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        AwanText(text = value, style = AwanTheme.styles.headingText)
        AwanText(text = label, style = AwanTheme.styles.captionText)
    }
}

@Composable
private fun PreferenceItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showDivider: Boolean = false,
    showArrow: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(text = title, style = AwanTheme.styles.headingText)
                    AwanText(text = subtitle, style = AwanTheme.styles.captionText)
                }
                if (showArrow) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AwanTheme.colors.meta
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = AwanTheme.colors.line,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    showDivider: Boolean = false
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AwanTheme.colors.background),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    AwanText(text = title, style = AwanTheme.styles.bodyText)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (value != null) {
                        AwanText(text = value, style = AwanTheme.styles.bodySecondaryText)
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AwanTheme.colors.meta,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 60.dp),
                    color = AwanTheme.colors.line,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
private fun ThemeOption(label: String, isSelected: Boolean) {
    AwanText(
        text = label,
        style = if (isSelected) AwanTheme.styles.buttonCompactText else AwanTheme.styles.bodySecondaryText,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun ProfileShimmer() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(200.dp), shape = AwanTheme.shapes.card)
        ShimmerItem(modifier = Modifier.width(100.dp).height(20.dp))
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(250.dp), shape = AwanTheme.shapes.card)
        ShimmerItem(modifier = Modifier.width(150.dp).height(20.dp))
        ShimmerItem(modifier = Modifier.fillMaxWidth().height(150.dp), shape = AwanTheme.shapes.card)
    }
}
