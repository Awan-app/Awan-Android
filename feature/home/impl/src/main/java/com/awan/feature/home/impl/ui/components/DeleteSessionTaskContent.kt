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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.home.impl.R
import com.awan.feature.home.impl.ui.DeleteTargetType

@Composable
internal fun DeleteSessionTaskContent(
    selectedTarget: DeleteTargetType,
    isDeleting: Boolean,
    onSelectTarget: (DeleteTargetType) -> Unit,
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
                    text = stringResource(R.string.home_delete_dialog_title),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                AwanText(
                    text = stringResource(R.string.home_delete_dialog_subtitle),
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 12.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DeleteOptionItemCard(
                icon = Icons.Default.Delete,
                title = stringResource(R.string.home_delete_option_session_title),
                subtitle = stringResource(R.string.home_delete_option_session_subtitle),
                isSelected = selectedTarget == DeleteTargetType.SESSION,
                onClick = { if (!isDeleting) onSelectTarget(DeleteTargetType.SESSION) },
            )

            DeleteOptionItemCard(
                icon = Icons.Default.DeleteForever,
                title = stringResource(R.string.home_delete_option_task_title),
                subtitle = stringResource(R.string.home_delete_option_task_subtitle),
                isSelected = selectedTarget == DeleteTargetType.TASK,
                onClick = { if (!isDeleting) onSelectTarget(DeleteTargetType.TASK) },
            )
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
                AwanText(text = stringResource(R.string.home_action_cancel))
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
                AwanText(text = stringResource(R.string.home_action_delete))
            }
        }
    }
}

@Composable
private fun DeleteOptionItemCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) AwanTheme.colors.destructive else AwanTheme.colors.line.copy(alpha = 0.5f)
    val bgColor = if (isSelected) AwanTheme.colors.destructive.copy(alpha = 0.08f) else AwanTheme.colors.surface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isSelected) AwanTheme.colors.destructive.copy(alpha = 0.15f)
                        else AwanTheme.colors.line.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) AwanTheme.colors.destructive else AwanTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = title,
                    style = AwanTheme.typography.body.copy(
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) AwanTheme.colors.destructive else AwanTheme.colors.textPrimary,
                    ),
                )
                AwanText(
                    text = subtitle,
                    style = AwanTheme.typography.caption.copy(
                        fontSize = 11.5.sp,
                        color = AwanTheme.colors.textSecondary,
                    ),
                )
            }
        }
    }
}
