package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.helpers.ProfileHelper
import com.awan.feature.profile.impl.R as ProfileR

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
    error: UiText? = null,
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
            value = "${ProfileHelper.formatDisplayTime(hour, minute)} ${stringResource(if (hour < 12) ProfileR.string.profile_am else ProfileR.string.profile_pm)}",
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
