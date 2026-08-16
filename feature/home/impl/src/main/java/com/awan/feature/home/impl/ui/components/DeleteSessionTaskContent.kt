package com.awan.feature.home.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R

@Composable
internal fun DeleteSessionTaskContent(
    isDeleting: Boolean,
    onConfirmDelete: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(AwanTheme.colors.destructive.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = AwanTheme.colors.destructive,
                    modifier = Modifier.size(22.dp),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                AwanText(
                    text = stringResource(R.string.home_delete_session_confirm_title),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                AwanText(
                    text = stringResource(R.string.home_delete_session_confirm_subtitle),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
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
                enabled = !isDeleting,
                modifier = Modifier.weight(1f),
            ) {
                AwanText(
                    text = stringResource(R.string.home_action_cancel),
                    style = AwanTheme.typography.button,
                )
            }

            AwanButton(
                onClick = onConfirmDelete,
                variant = AwanButtonVariant.Destructive,
                enabled = !isDeleting,
                isLoading = isDeleting,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                modifier = Modifier.weight(1f),
            ) {
                AwanText(
                    text = stringResource(R.string.home_action_delete),
                    style = AwanTheme.typography.button,
                )
            }
        }
    }
}

