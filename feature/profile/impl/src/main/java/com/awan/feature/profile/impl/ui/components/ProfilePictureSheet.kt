package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.profile.impl.R as ProfileR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePictureSheet(
    onDismiss: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.textSecondary.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AwanTheme.spacing.xxl)
        ) {
            AwanText(
                text = stringResource(ProfileR.string.profile_photo_sheet_title),
                style = AwanTheme.styles.titleText,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                PhotoOption(
                    icon = Icons.Default.PhotoCamera,
                    label = stringResource(ProfileR.string.profile_camera),
                    onClick = {
                        onCameraClick()
                        onDismiss()
                    }
                )
                PhotoOption(
                    icon = Icons.Default.PhotoLibrary,
                    label = stringResource(ProfileR.string.profile_gallery),
                    onClick = {
                        onGalleryClick()
                        onDismiss()
                    }
                )
                if (onDeleteClick != null) {
                    PhotoOption(
                        icon = Icons.Default.Delete,
                        label = stringResource(ProfileR.string.profile_remove_photo),
                        onClick = {
                            onDeleteClick()
                            onDismiss()
                        },
                        isDestructive = true
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = if (isDestructive) AwanTheme.colors.destructive.copy(alpha = 0.1f) else AwanTheme.colors.sky.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isDestructive) AwanTheme.colors.destructive else AwanTheme.colors.sky,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        AwanText(
            text = label,
            style = AwanTheme.styles.bodySecondaryText.copy(
                color = if (isDestructive) AwanTheme.colors.destructive else AwanTheme.colors.textPrimary
            )
        )
    }
}
