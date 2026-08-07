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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import com.awan.feature.home.impl.R

@Composable
internal fun EditSessionTaskContent(
    editTitle: String,
    editDescription: String,
    editDurationMinutes: Int,
    isSaving: Boolean,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDurationChange: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AwanText(
                text = stringResource(R.string.home_edit_session_title),
                style = AwanTheme.typography.heading.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AwanTheme.colors.textPrimary,
                ),
            )
        }

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

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AwanButton(
                onClick = onCancel,
                variant = AwanButtonVariant.Secondary,
                modifier = Modifier.weight(1f),
            ) {
                AwanText(text = stringResource(R.string.home_action_cancel))
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
                    AwanText(text = stringResource(R.string.home_action_save))
                }
            }
        }
    }
}
