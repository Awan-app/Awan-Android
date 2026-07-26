package com.awan.feature.profile.impl.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.helpers.ProfileHelper
import com.awan.feature.profile.impl.R as ProfileR
import com.awan.feature.profile.impl.presentation.EditProfileUiState
import com.awan.feature.profile.impl.ui.components.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    uiState: EditProfileUiState,
    onFirstNameChange: (String) -> Unit,
    onLastNameChange: (String) -> Unit,
    onBirthDateChange: (String) -> Unit,
    onUpdateSleepSchedule: (String, String) -> Unit,
    onUpdateSessionDuration: (Int) -> Unit,
    onUpdateSchedulingType: (String) -> Unit,
    onUpdateTimezone: (String) -> Unit,
    onSaveClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedItem by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(uiState.isSuccess) {
        if (uiState.isSuccess) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    AwanText(
                        text = stringResource(ProfileR.string.profile_edit_title),
                        style = AwanTheme.styles.titleText
                    )
                },
                navigationIcon = {
                    AwanIconButton(
                        onClick = onBackClick,
                        contentDescription = stringResource(ProfileR.string.profile_back)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = AwanTheme.colors.textPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = AwanTheme.colors.background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Section 1: Personal Information
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(ProfileR.string.profile_section_personal))
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            EditField(
                                label = stringResource(ProfileR.string.profile_first_name),
                                value = uiState.firstName,
                                onValueChange = onFirstNameChange,
                                placeholder = stringResource(ProfileR.string.profile_first_name)
                            )
                            EditField(
                                label = stringResource(ProfileR.string.profile_last_name),
                                value = uiState.lastName,
                                onValueChange = onLastNameChange,
                                placeholder = stringResource(ProfileR.string.profile_last_name)
                            )
                            PreferenceRow(
                                icon = Icons.Default.Cake,
                                title = stringResource(ProfileR.string.profile_birth_date),
                                value = uiState.birthDate.ifBlank { stringResource(ProfileR.string.profile_select_date) },
                                onClick = { showDatePicker = true },
                                iconColor = AwanTheme.colors.zoneTangerine
                            )
                        }
                    }
                }

                // Section 2: Daily Schedule
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(ProfileR.string.profile_section_daily_schedule))
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        ExpandableTimePickerItem(
                            icon = Icons.Default.WbSunny,
                            title = stringResource(ProfileR.string.profile_wakeup_time),
                            hour = ProfileHelper.parseHour(uiState.wakeupTime),
                            minute = ProfileHelper.parseMinute(uiState.wakeupTime),
                            isExpanded = expandedItem == "wakeup",
                            onExpandClick = { expandedItem = if (expandedItem == "wakeup") null else "wakeup" },
                            onSaveClick = { h, m ->
                                onUpdateSleepSchedule(ProfileHelper.formatToApiTime(h, m), uiState.sleepTime)
                                expandedItem = null
                            },
                            onCancelClick = { expandedItem = null },
                            isLoading = uiState.isUpdatingField,
                            showDivider = true
                        )

                        ExpandableTimePickerItem(
                            icon = Icons.Default.NightsStay,
                            title = stringResource(ProfileR.string.profile_sleep_time),
                            hour = ProfileHelper.parseHour(uiState.sleepTime),
                            minute = ProfileHelper.parseMinute(uiState.sleepTime),
                            isExpanded = expandedItem == "sleep",
                            onExpandClick = { expandedItem = if (expandedItem == "sleep") null else "sleep" },
                            onSaveClick = { h, m ->
                                onUpdateSleepSchedule(uiState.wakeupTime, ProfileHelper.formatToApiTime(h, m))
                                expandedItem = null
                            },
                            onCancelClick = { expandedItem = null },
                            isLoading = uiState.isUpdatingField,
                            showDivider = false
                        )
                    }
                }

                // Section 3: Preferences
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SectionTitle(stringResource(ProfileR.string.profile_section_preferences))
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        PreferenceRow(
                            icon = Icons.Default.Public,
                            title = stringResource(ProfileR.string.profile_timezone),
                            value = uiState.timezone,
                            onClick = { onUpdateTimezone(uiState.timezone) /* Example placeholder */ },
                            showDivider = true,
                            iconColor = AwanTheme.colors.sky
                        )
                        
                        PreferenceRow(
                            icon = Icons.Default.Timer,
                            title = stringResource(ProfileR.string.profile_session_duration),
                            iconColor = AwanTheme.colors.zoneSun,
                            showDivider = true
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                AwanIconButton(
                                    onClick = { onUpdateSessionDuration((uiState.preferredSessionDuration - 5).coerceAtLeast(5)) },
                                    contentDescription = stringResource(ProfileR.string.profile_decrease)
                                ) {
                                    Icon(Icons.Default.Remove, null, tint = AwanTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                                }
                                AwanText(
                                    text = stringResource(ProfileR.string.profile_session_time_value, uiState.preferredSessionDuration),
                                    style = AwanTheme.styles.bodyText.copy(textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.Bold))
                                )
                                AwanIconButton(
                                    onClick = { onUpdateSessionDuration((uiState.preferredSessionDuration + 5).coerceAtMost(180)) },
                                    contentDescription = stringResource(ProfileR.string.profile_increase)
                                ) {
                                    Icon(Icons.Default.Add, null, tint = AwanTheme.colors.textPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier.size(32.dp).clip(androidx.compose.foundation.shape.CircleShape).background(AwanTheme.colors.zoneViolet.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Tune, null, tint = AwanTheme.colors.zoneViolet, modifier = Modifier.size(18.dp))
                                }
                                AwanText(stringResource(ProfileR.string.profile_scheduling_type), style = AwanTheme.styles.bodyText.copy(textStyle = AwanTheme.typography.body.copy(fontWeight = FontWeight.SemiBold)))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                SchedulingOption("SMART", stringResource(ProfileR.string.profile_scheduling_type_smart), uiState.schedulingType == "SMART", onUpdateSchedulingType)
                                SchedulingOption("MANUAL", stringResource(ProfileR.string.profile_scheduling_type_manual), uiState.schedulingType == "MANUAL", onUpdateSchedulingType)
                                SchedulingOption("FIXED", stringResource(ProfileR.string.profile_scheduling_type_fixed), uiState.schedulingType == "FIXED", onUpdateSchedulingType)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(80.dp))
            }

            // Bottom Save Button
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(AwanTheme.colors.background.copy(alpha = 0.9f))
                    .padding(20.dp)
            ) {
                AwanButton(
                    onClick = onSaveClick,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving && uiState.firstName.isNotBlank() && uiState.lastName.isNotBlank()
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = AwanTheme.colors.onSky,
                            strokeWidth = 2.dp
                        )
                    } else {
                        AwanText(text = stringResource(ProfileR.string.profile_save_changes))
                    }
                }
            }

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AwanTheme.colors.sky)
                }
            }
        }
    }

    val birthDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                birthDateFormat.parse(uiState.birthDate)?.time
            } catch (e: Exception) {
                null
            }
        )
        AwanDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AwanButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onBirthDateChange(birthDateFormat.format(Date(millis)))
                        }
                        showDatePicker = false
                    },
                    variant = AwanButtonVariant.Quiet
                ) {
                    AwanText(stringResource(ProfileR.string.profile_ok))
                }
            },
            dismissButton = {
                AwanButton(
                    onClick = { showDatePicker = false },
                    variant = AwanButtonVariant.Quiet
                ) {
                    AwanText(stringResource(ProfileR.string.profile_cancel))
                }
            }
        ) {
            AwanDatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun EditField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AwanText(
            text = label,
            style = AwanTheme.styles.captionText.copy(
                textStyle = AwanTheme.typography.caption.copy(
                    color = AwanTheme.colors.meta,
                    fontWeight = FontWeight.Bold
                )
            )
        )
        AwanTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SchedulingOption(
    id: String,
    label: String,
    isSelected: Boolean,
    onClick: (String) -> Unit
) {
    Surface(
        onClick = { onClick(id) },
        shape = AwanTheme.shapes.pill,
        color = if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.15f) else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(
            1.dp, 
            if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line
        ),
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            AwanText(
                text = label,
                style = AwanTheme.styles.captionText.copy(
                    textStyle = AwanTheme.typography.caption.copy(
                        color = if (isSelected) AwanTheme.colors.skyPressed else AwanTheme.colors.textSecondary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                )
            )
        }
    }
}
