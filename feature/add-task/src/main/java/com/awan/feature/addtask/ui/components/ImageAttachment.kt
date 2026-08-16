package com.awan.feature.addtask.ui.components

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.addtask.R
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Image as LucideImageIcon
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import java.io.File

private val ThumbnailSize = 72.dp

private fun Context.findActivity(): ComponentActivity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is ComponentActivity) return current
        current = current.baseContext
    }
    return null
}

/**
 * A photo of a whiteboard, a handwritten list, or a screenshot — extra context Awan reads alongside
 * the typed note. Never both empty and filled: showing the picker row or the attached thumbnail, not
 * both at once.
 */
@Composable
fun ImageAttachment(
    imageUri: String?,
    onImagePicked: (String) -> Unit,
    onImageCleared: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (imageUri != null) {
        AttachedImage(uri = imageUri, onRemove = onImageCleared, modifier = modifier)
    } else {
        PickImageButtons(onImagePicked = onImagePicked, modifier = modifier)
    }
}

@Composable
private fun PickImageButtons(onImagePicked: (String) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showCameraSettingsDialog by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onImagePicked(it.toString()) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) pendingCameraUri?.let { onImagePicked(it.toString()) }
    }

    fun launchCameraInternal() {
        val uri = createCameraOutputUri(context)
        pendingCameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            val activity = context.findActivity()
            val showRationale = activity != null && ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.CAMERA
            )
            if (showRationale) {
                showCameraRationaleDialog = true
            } else {
                showCameraSettingsDialog = true
            }
        }
    }

    fun requestCameraPermissionAndLaunch() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            launchCameraInternal()
        } else {
            val activity = context.findActivity()
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showCameraRationaleDialog = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
        AwanText(stringResource(R.string.add_task_add_photo), style = AwanTheme.styles.metaText)
        Row(horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
            AwanButton(
                onClick = {
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                variant = AwanButtonVariant.Quiet,
                icon = { Icon(Lucide.LucideImageIcon, contentDescription = null) },
            ) {
                AwanText(stringResource(R.string.add_task_photo_from_gallery))
            }
            AwanButton(
                onClick = {
                    requestCameraPermissionAndLaunch()
                },
                variant = AwanButtonVariant.Quiet,
                icon = { Icon(Lucide.Camera, contentDescription = null) },
            ) {
                AwanText(stringResource(R.string.add_task_photo_take))
            }
        }
    }

    if (showCameraRationaleDialog) {
        AwanDialog(
            title = stringResource(R.string.add_task_camera_permission_rationale_title),
            body = stringResource(R.string.add_task_camera_permission_rationale_message),
            primaryLabel = stringResource(R.string.add_task_grant_permission),
            onPrimary = {
                showCameraRationaleDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            secondaryLabel = stringResource(R.string.add_task_cancel),
            onSecondary = { showCameraRationaleDialog = false },
            onDismiss = { showCameraRationaleDialog = false }
        )
    }

    if (showCameraSettingsDialog) {
        AwanDialog(
            title = stringResource(R.string.add_task_camera_permission_settings_title),
            body = stringResource(R.string.add_task_camera_permission_settings_message),
            primaryLabel = stringResource(R.string.add_task_open_settings),
            onPrimary = {
                showCameraSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            secondaryLabel = stringResource(R.string.add_task_cancel),
            onSecondary = { showCameraSettingsDialog = false },
            onDismiss = { showCameraSettingsDialog = false }
        )
    }
}

@Composable
private fun AttachedImage(uri: String, onRemove: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        AwanUriImage(
            uri = uri,
            contentDescription = stringResource(R.string.add_task_photo_content_description),
            modifier = Modifier.size(ThumbnailSize).clip(RoundedCornerShape(12.dp)),
        )
        AwanIconButton(
            onClick = onRemove,
            contentDescription = stringResource(R.string.add_task_remove_photo),
            modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
        ) {
            Icon(imageVector = Lucide.X, contentDescription = null, tint = AwanTheme.colors.textPrimary)
        }
    }
}
