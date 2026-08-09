package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.profile.model.Profile
import com.awan.feature.profile.impl.helpers.ProfileHelper
import com.awan.feature.profile.impl.presentation.ProfileState
import com.awan.feature.profile.impl.R as ProfileR

@Composable
fun PreferencesCard(
    profile: Profile,
    uiState: ProfileState,
    onDailyZonesClick: () -> Unit,
    onCategoryManagementClick: () -> Unit,
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

            PreferenceRow(
                icon = Icons.Default.Category,
                title = stringResource(ProfileR.string.profile_categories_title),
                onClick = onCategoryManagementClick,
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

            ExpandableTimePickerItem(
                icon = Icons.Default.WbSunny,
                title = stringResource(ProfileR.string.profile_wakeup_time),
                hour = ProfileHelper.parseHour(profile.preferences?.wakeupTime) ?: 7,
                minute = ProfileHelper.parseMinute(profile.preferences?.wakeupTime) ?: 30,
                isExpanded = expandedItem == "wakeup",
                onExpandClick = { expandedItem = if (expandedItem == "wakeup") null else "wakeup" },
                onSaveClick = { h, m ->
                    onUpdateSleepSchedule(ProfileHelper.formatToApiTime(h, m), profile.preferences?.sleepTime ?: "23:00:00")
                    expandedItem = null
                },
                onCancelClick = { expandedItem = null },
                isLoading = uiState.isUpdatingField,
                showDivider = true
            )

            ExpandableTimePickerItem(
                icon = Icons.Default.NightsStay,
                title = stringResource(ProfileR.string.profile_sleep_time),
                hour = ProfileHelper.parseHour(profile.preferences?.sleepTime) ?: 23,
                minute = ProfileHelper.parseMinute(profile.preferences?.sleepTime) ?: 0,
                isExpanded = expandedItem == "sleep",
                onExpandClick = { expandedItem = if (expandedItem == "sleep") null else "sleep" },
                onSaveClick = { h, m ->
                    onUpdateSleepSchedule(profile.preferences?.wakeupTime ?: "07:30:00", ProfileHelper.formatToApiTime(h, m))
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
                },
                isLoading = uiState.isUpdatingField,
                showDivider = true
            )
        }
    }
}
