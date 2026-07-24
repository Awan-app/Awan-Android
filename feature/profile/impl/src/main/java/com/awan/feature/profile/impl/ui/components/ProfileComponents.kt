package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR
import java.util.TimeZone

@Composable
fun SectionTitle(title: String) {
    AwanText(
        text = title,
        style = AwanTheme.styles.captionText.copy(
            color = AwanTheme.colors.meta,
            textStyle = AwanTheme.typography.caption.copy(
                letterSpacing = 1.2.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
        ),
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
fun PreferenceRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = false,
    iconColor: Color = AwanTheme.colors.sky,
    isExpanded: Boolean = false,
    content: @Composable (() -> Unit)? = null
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "rotation"
    )

    Surface(
        onClick = { onClick?.invoke() },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
        enabled = onClick != null
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(iconColor.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        AwanText(
                            text = title,
                            style = AwanTheme.styles.bodyText.copy(
                                textStyle = AwanTheme.typography.body.copy(
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                )
                            )
                        )
                        if (value != null && !isExpanded) {
                            AwanText(
                                text = value,
                                style = AwanTheme.styles.bodySecondaryText
                            )
                        }
                    }
                }

                if (content != null) {
                    content()
                } else {
                    Icon(
                        imageVector = if (onClick != null && value != null) Icons.Default.ExpandMore else Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = AwanTheme.colors.meta,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(if (onClick != null && value != null) rotationState else 0f)
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AwanTheme.colors.line.copy(alpha = 0.5f),
                    thickness = 1.dp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpandableTimePickerItem(
    icon: ImageVector,
    title: String,
    hour: Int,
    minute: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onSaveClick: (Int, Int) -> Unit,
    onCancelClick: () -> Unit,
    isLoading: Boolean = false,
    error: com.awan.app.core.common.text.UiText? = null,
    showDivider: Boolean = false
) {
    val timePickerState = rememberTimePickerState(
        initialHour = hour,
        initialMinute = minute,
        is24Hour = false
    )

    key(isExpanded, hour, minute) { }

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = icon,
            title = title,
            value = formatTime(hour, minute),
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (error != null) {
                    AwanText(
                        text = error.asString(),
                        style = AwanTheme.styles.errorText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = AwanTheme.colors.background,
                        clockDialSelectedContentColor = AwanTheme.colors.onSky,
                        clockDialUnselectedContentColor = AwanTheme.colors.textPrimary,
                        selectorColor = AwanTheme.colors.sky,
                        periodSelectorBorderColor = AwanTheme.colors.line,
                        periodSelectorSelectedContainerColor = AwanTheme.colors.sky.copy(alpha = 0.2f),
                        periodSelectorSelectedContentColor = AwanTheme.colors.sky,
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorUnselectedContentColor = AwanTheme.colors.textSecondary,
                        timeSelectorSelectedContainerColor = AwanTheme.colors.sky.copy(alpha = 0.2f),
                        timeSelectorSelectedContentColor = AwanTheme.colors.sky,
                        timeSelectorUnselectedContainerColor = AwanTheme.colors.background,
                        timeSelectorUnselectedContentColor = AwanTheme.colors.textPrimary,
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_cancel),
                            style = AwanTheme.styles.buttonCompactText.copy(color = AwanTheme.colors.meta)
                        )
                    }
                    AwanButton(
                        onClick = { onSaveClick(timePickerState.hour, timePickerState.minute) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AwanTheme.colors.onSky,
                                strokeWidth = 2.dp
                            )
                        } else {
                            AwanText(text = stringResource(ProfileR.string.profile_save))
                        }
                    }
                }
            }
        }
        if (showDivider && isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun ExpandableSessionDurationItem(
    duration: Int,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onSaveClick: (Int) -> Unit,
    onCancelClick: () -> Unit,
    isLoading: Boolean = false,
    showDivider: Boolean = false
) {
    var localDuration by remember(duration, isExpanded) { mutableStateOf(duration) }

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = Icons.Default.Timer,
            title = stringResource(ProfileR.string.profile_session_time),
            value = stringResource(ProfileR.string.profile_session_time_value, duration),
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded,
            iconColor = AwanTheme.colors.zoneSun
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    AwanIconButton(
                        onClick = { localDuration = (localDuration - 5).coerceAtLeast(5) },
                        contentDescription = "Decrease",
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Remove, null, tint = AwanTheme.colors.textPrimary)
                    }
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        AwanText(
                            text = localDuration.toString(),
                            style = AwanTheme.styles.headingText.copy(
                                textStyle = AwanTheme.typography.heading.copy(fontSize = 32.sp)
                            )
                        )
                        AwanText(
                            text = "minutes",
                            style = AwanTheme.styles.captionText
                        )
                    }

                    AwanIconButton(
                        onClick = { localDuration = (localDuration + 5).coerceAtMost(180) },
                        contentDescription = "Increase",
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = AwanTheme.colors.textPrimary)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onCancelClick,
                        modifier = Modifier.weight(1f)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_cancel),
                            style = AwanTheme.styles.buttonCompactText.copy(color = AwanTheme.colors.meta)
                        )
                    }
                    AwanButton(
                        onClick = { onSaveClick(localDuration) },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AwanTheme.colors.onSky,
                                strokeWidth = 2.dp
                            )
                        } else {
                            AwanText(text = stringResource(ProfileR.string.profile_save))
                        }
                    }
                }
            }
        }
        
        if (showDivider && isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun DailyZonesItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    zoneCount: Int = 0,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.background),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AwanText(
                            text = title,
                            style = AwanTheme.styles.titleText.copy(
                                textStyle = AwanTheme.typography.title.copy(fontSize = 18.sp)
                            )
                        )
                        if (zoneCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AwanTheme.colors.sky.copy(alpha = 0.1f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                AwanText(
                                    text = "$zoneCount",
                                    style = AwanTheme.styles.captionText.copy(
                                        color = AwanTheme.colors.sky,
                                        textStyle = AwanTheme.typography.caption.copy(fontWeight = FontWeight.Bold)
                                    )
                                )
                            }
                        }
                    }
                    AwanText(
                        text = subtitle,
                        style = AwanTheme.styles.bodySecondaryText
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneViolet))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneSky))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneSun))
                        Box(modifier = Modifier.weight(1f).fillMaxHeight().background(AwanTheme.colors.zoneCoral))
                    }
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = AwanTheme.colors.meta
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AwanTheme.colors.line,
                    thickness = 1.dp
                )
            }
        }
    }
}

@Composable
fun ThemeOption(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        AwanText(
            text = label,
            style = if (isSelected) {
                AwanTheme.styles.buttonCompactText
            } else {
                AwanTheme.styles.bodySecondaryText
            },
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AwanText(text = "Select Language", style = AwanTheme.styles.titleText) },
        text = {
            Column {
                LanguageOption("English", "en", currentLanguage == "en" || currentLanguage == "", onLanguageSelected)
                LanguageOption("العربية", "ar", currentLanguage == "ar", onLanguageSelected)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Cancel", style = AwanTheme.styles.buttonCompactText)
            }
        },
        containerColor = AwanTheme.colors.surface,
        shape = AwanTheme.shapes.card
    )
}

@Composable
private fun LanguageOption(
    label: String,
    code: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AwanTheme.colors.background else Color.Transparent)
            .clickable { onSelect(code) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = isSelected, onClick = { onSelect(code) })
        AwanText(text = label, style = AwanTheme.styles.bodyText)
    }
}

@Composable
fun TimezoneSelectionDialog(
    currentSelection: String,
    onTimezoneSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val timezones = remember { 
        TimeZone.getAvailableIDs()
            .filter { it.contains("/") }
            .sortedBy { it }
    }
    var searchQuery by remember { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery) {
        if (searchQuery.isBlank()) timezones
        else timezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AwanText(text = "Select Timezone", style = AwanTheme.styles.titleText)
                AwanTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search region or city...",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        text = {
            Box(modifier = Modifier.height(300.dp)) {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTimezones.size) { index ->
                        val tz = filteredTimezones[index]
                        val isSelected = tz == currentSelection
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                                .clickable { 
                                    onTimezoneSelected(tz)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            AwanText(
                                text = tz.replace("_", " "),
                                style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                            )
                            if (isSelected) {
                                Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = "Close", style = AwanTheme.styles.buttonCompactText)
            }
        },
        containerColor = AwanTheme.colors.surface,
        shape = AwanTheme.shapes.card
    )
}

@Composable
fun ExpandableTimezoneItem(
    currentSelection: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onTimezoneSelected: (String) -> Unit,
    isLoading: Boolean = false,
    showDivider: Boolean = false
) {
    val timezones = remember {
        TimeZone.getAvailableIDs()
            .filter { it.contains("/") }
            .sortedBy { it }
    }
    var searchQuery by remember(isExpanded) { mutableStateOf("") }
    val filteredTimezones = remember(searchQuery) {
        if (searchQuery.isBlank()) timezones
        else timezones.filter { it.contains(searchQuery, ignoreCase = true) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = Icons.Default.Public,
            title = stringResource(ProfileR.string.profile_time_zone),
            value = currentSelection.replace("_", " "),
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded,
            iconColor = AwanTheme.colors.sky
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AwanTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = "Search region or city...",
                    modifier = Modifier.fillMaxWidth()
                )

                Box(modifier = Modifier.height(240.dp)) {
                    androidx.compose.foundation.lazy.LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredTimezones.size) { index ->
                            val tz = filteredTimezones[index]
                            val isSelected = tz == currentSelection
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                                    .clickable { onTimezoneSelected(tz) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                AwanText(
                                    text = tz.replace("_", " "),
                                    style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                                )
                                if (isSelected) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
        if (showDivider && isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun ExpandableLanguageItem(
    currentLanguage: String,
    isExpanded: Boolean,
    onExpandClick: () -> Unit,
    onLanguageSelected: (String) -> Unit,
    showDivider: Boolean = false
) {
    val languages = listOf(
        "en" to "English",
        "ar" to "العربية"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        PreferenceRow(
            icon = Icons.Default.Language,
            title = stringResource(ProfileR.string.profile_language),
            value = languages.find { it.first == currentLanguage }?.second ?: "English",
            onClick = onExpandClick,
            showDivider = showDivider && !isExpanded,
            isExpanded = isExpanded,
            iconColor = AwanTheme.colors.zoneSun
        )

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                languages.forEach { (code, name) ->
                    val isSelected = code == currentLanguage
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.1f) else Color.Transparent)
                            .clickable { 
                                onLanguageSelected(code)
                                // We don't collapse immediately so user sees the checkmark move
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        AwanText(
                            text = name,
                            style = if (isSelected) AwanTheme.styles.bodyText.copy(color = AwanTheme.colors.skyPressed) else AwanTheme.styles.bodyText
                        )
                        if (isSelected) {
                            Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
        
        if (showDivider && isExpanded) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = AwanTheme.colors.line.copy(alpha = 0.5f),
                thickness = 1.dp
            )
        }
    }
}

private fun formatTime(hour: Int, minute: Int): String {
    val amPm = if (hour < 12) "AM" else "PM"
    val h = if (hour % 12 == 0) 12 else hour % 12
    return String.format(java.util.Locale.getDefault(), "%02d:%02d %s", h, minute, amPm)
}
