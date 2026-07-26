package com.awan.feature.profile.impl.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR

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
                        contentDescription = stringResource(ProfileR.string.profile_decrease),
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
                            text = stringResource(ProfileR.string.profile_minutes),
                            style = AwanTheme.styles.captionText
                        )
                    }

                    AwanIconButton(
                        onClick = { localDuration = (localDuration + 5).coerceAtMost(180) },
                        contentDescription = stringResource(ProfileR.string.profile_increase),
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
