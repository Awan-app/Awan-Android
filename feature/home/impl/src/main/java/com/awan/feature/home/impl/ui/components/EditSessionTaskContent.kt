package com.awan.feature.home.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextField
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.designsystem.ScheduleZone
import com.awan.app.core.designsystem.formatTime
import com.awan.feature.home.impl.R

@Composable
internal fun EditSessionTaskContent(
    editTitle: String,
    editDescription: String,
    editStartMinutes: Int,
    editEndMinutes: Int,
    editDurationMinutes: Int,
    editZoneId: String?,
    availableZones: List<ScheduleZone>,
    isSaving: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onStartMinutesChange: (Int) -> Unit,
    onEndMinutesChange: (Int) -> Unit,
    onDurationChange: (Int) -> Unit,
    onZoneChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AwanText(
            text = stringResource(R.string.home_edit_session_title),
            style = AwanTheme.typography.heading.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AwanTheme.colors.textPrimary,
            ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Task Details Editing Section
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = stringResource(R.string.home_edit_label_title),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
                AwanTextField(
                    value = editTitle,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.home_edit_placeholder_title),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = stringResource(R.string.home_edit_label_description),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
                AwanTextField(
                    value = editDescription,
                    onValueChange = onDescriptionChange,
                    placeholder = stringResource(R.string.home_edit_placeholder_description),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            HorizontalDivider(color = AwanTheme.colors.line.copy(alpha = 0.4f))

            // Session Time & Zone Editing Section
            AwanText(
                text = stringResource(R.string.home_session_details_title).uppercase(),
                style = AwanTheme.typography.caption.copy(
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AwanTheme.colors.textSecondary,
                    letterSpacing = 0.8.sp,
                ),
            )

            // Start Time and End Time Steppers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Start Time Adjuster
                TimeStepperCard(
                    label = stringResource(R.string.home_edit_label_start_time),
                    minutesValue = editStartMinutes,
                    onDecrease = { onStartMinutesChange(editStartMinutes - 15) },
                    onIncrease = { onStartMinutesChange(editStartMinutes + 15) },
                    modifier = Modifier.weight(1f),
                )

                // End Time Adjuster
                TimeStepperCard(
                    label = stringResource(R.string.home_edit_label_end_time),
                    minutesValue = editEndMinutes,
                    onDecrease = { onEndMinutesChange(editEndMinutes - 15) },
                    onIncrease = { onEndMinutesChange(editEndMinutes + 15) },
                    modifier = Modifier.weight(1f),
                )
            }

            // Duration Selector Chips
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(
                    text = stringResource(R.string.home_edit_label_duration),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
                val durationOptions = listOf(15, 30, 45, 60, 90, 120)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    durationOptions.forEach { duration ->
                        val isSelected = editDurationMinutes == duration
                        val backgroundColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.surface
                        val textColor = if (isSelected) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(backgroundColor)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .clickable { onDurationChange(duration) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AwanText(
                                text = "${duration}m",
                                style = AwanTheme.typography.caption.copy(
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    textAlign = TextAlign.Center,
                                ),
                            )
                        }
                    }
                }
            }

            // Target Zone Selection Chips
            if (availableZones.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AwanText(
                        text = stringResource(R.string.home_edit_label_zone),
                        style = AwanTheme.typography.caption.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AwanTheme.colors.textSecondary,
                        ),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        availableZones.forEach { zone ->
                            val isSelected = editZoneId == zone.id
                            val zoneName = zone.category.name
                            val backgroundColor = if (isSelected) AwanTheme.colors.sky.copy(alpha = 0.15f) else AwanTheme.colors.surface
                            val textColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textPrimary
                            val borderColor = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.line.copy(alpha = 0.6f)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(backgroundColor)
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = borderColor,
                                        shape = RoundedCornerShape(10.dp),
                                    )
                                    .clickable { onZoneChange(zone.id) }
                                    .padding(vertical = 8.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                AwanText(
                                    text = zoneName,
                                    style = AwanTheme.typography.caption.copy(
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                    ),
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AwanButton(
                onClick = onCancel,
                variant = AwanButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            ) {
                AwanText(
                    text = stringResource(R.string.home_action_cancel),
                    style = AwanTheme.typography.button,
                )
            }

            AwanButton(
                onClick = onSave,
                variant = AwanButtonVariant.Primary,
                enabled = !isSaving && editTitle.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = AwanTheme.colors.onSky,
                        strokeWidth = 2.dp,
                    )
                } else {
                    AwanText(
                        text = stringResource(R.string.home_action_save),
                        style = AwanTheme.typography.button,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeStepperCard(
    label: String,
    minutesValue: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formattedStr = formatTime(minutesValue)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(AwanTheme.colors.surface)
            .border(
                width = 1.dp,
                color = AwanTheme.colors.line.copy(alpha = 0.6f),
                shape = RoundedCornerShape(14.dp),
            )
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AwanText(
            text = label,
            style = AwanTheme.typography.caption.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AwanTheme.colors.textSecondary,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.line.copy(alpha = 0.25f))
                    .clickable(onClick = onDecrease),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }

            AwanText(
                text = formattedStr,
                style = AwanTheme.typography.body.copy(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )

            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.line.copy(alpha = 0.25f))
                    .clickable(onClick = onIncrease),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = AwanTheme.colors.textPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

