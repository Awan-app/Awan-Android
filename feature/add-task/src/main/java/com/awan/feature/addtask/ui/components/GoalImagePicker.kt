package com.awan.feature.addtask.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image as ComposeImage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanButtonVariant
import com.awan.app.core.designsystem.AwanDisclosure
import com.awan.app.core.designsystem.AwanIconButton
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.feature.addtask.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.X
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val THUMBNAIL_SIZE_PX = 256

@Composable
internal fun GoalImagePicker(
    imageUri: String?,
    enabled: Boolean,
    onImageChanged: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var sourceSelectorExpanded by rememberSaveable { mutableStateOf(false) }
    var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { onImageChanged(it.toString()) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { saved ->
        pendingCameraUri?.takeIf { saved }?.let(onImageChanged)
        pendingCameraUri = null
    }

    if (imageUri == null) {
        AwanDisclosure(
            expanded = sourceSelectorExpanded,
            onExpandedChange = { sourceSelectorExpanded = it },
            label = stringResource(R.string.add_task_goal_image_add),
            stateDescription = stringResource(
                if (sourceSelectorExpanded) {
                    R.string.add_task_goal_image_sources_expanded
                } else {
                    R.string.add_task_goal_image_sources_collapsed
                },
            ),
            icon = Lucide.Image,
            enabled = enabled,
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
            ) {
                AwanButton(
                    onClick = {
                        sourceSelectorExpanded = false
                        val uri = newGoalImageUri(context)
                        pendingCameraUri = uri.toString()
                        cameraLauncher.launch(uri)
                    },
                    variant = AwanButtonVariant.Secondary,
                    icon = Lucide.Camera,
                    modifier = Modifier.weight(1f),
                ) {
                    AwanText(stringResource(R.string.add_task_goal_image_camera))
                }
                AwanButton(
                    onClick = {
                        sourceSelectorExpanded = false
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    variant = AwanButtonVariant.Secondary,
                    icon = Lucide.Image,
                    modifier = Modifier.weight(1f),
                ) {
                    AwanText(stringResource(R.string.add_task_goal_image_library))
                }
            }
        }
    } else {
        SelectedGoalImage(
            imageUri = imageUri,
            onRemove = { onImageChanged(null) },
            modifier = modifier,
        )
    }
}

@Composable
private fun SelectedGoalImage(
    imageUri: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnail = rememberGoalThumbnail(imageUri)
    AwanCard(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm),
        ) {
            if (thumbnail != null) {
                ComposeImage(
                    bitmap = thumbnail,
                    contentDescription = stringResource(R.string.add_task_goal_image_preview_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(AwanTheme.shapes.card),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                AwanText(
                    text = stringResource(R.string.add_task_goal_image_selected),
                    style = AwanTheme.typography.body.copy(color = AwanTheme.colors.textPrimary),
                )
            }
            AwanIconButton(
                onClick = onRemove,
                contentDescription = stringResource(R.string.add_task_goal_image_remove),
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Lucide.X,
                    contentDescription = null,
                    tint = AwanTheme.colors.textSecondary,
                )
            }
        }
    }
}

@Composable
private fun rememberGoalThumbnail(uriString: String): ImageBitmap? {
    val context = LocalContext.current
    val uri = remember(uriString) { Uri.parse(uriString) }
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(context, uri) }
    }
    return bitmap?.asImageBitmap()
}

private fun newGoalImageUri(context: Context): Uri {
    val directory = File(context.cacheDir, "goal-images").apply { mkdirs() }
    val image = File.createTempFile("goal-", ".jpg", directory)
    return FileProvider.getUriForFile(context, "${context.packageName}.goalimage", image)
}

private fun decodeThumbnail(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > THUMBNAIL_SIZE_PX ||
        bounds.outHeight / sampleSize > THUMBNAIL_SIZE_PX
    ) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
}
