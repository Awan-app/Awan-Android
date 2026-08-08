package com.awan.feature.addtask.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { onImagePicked(it.toString()) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captured ->
        if (captured) pendingCameraUri?.let { onImagePicked(it.toString()) }
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
                    val uri = createCameraOutputUri(context)
                    pendingCameraUri = uri
                    cameraLauncher.launch(uri)
                },
                variant = AwanButtonVariant.Quiet,
                icon = { Icon(Lucide.Camera, contentDescription = null) },
            ) {
                AwanText(stringResource(R.string.add_task_photo_take))
            }
        }
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
