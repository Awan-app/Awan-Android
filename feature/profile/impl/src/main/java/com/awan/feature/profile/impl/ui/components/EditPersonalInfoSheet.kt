package com.awan.feature.profile.impl.ui.components

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.awan.app.core.common.text.UiText
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalInfoSheet(
    initialFirstName: String,
    initialLastName: String,
    initialBirthDate: String,
    profilePictureUrl: String?,
    pendingPictureUri: String?,
    error: UiText?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    onPickPicture: (String) -> Unit,
    onDeletePicture: () -> Unit,
    isLoading: Boolean = false
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showCameraRationaleDialog by remember { mutableStateOf(false) }
    var showCameraSettingsDialog by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { onPickPicture(it.toString()) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempPhotoUri?.let { onPickPicture(it.toString()) }
        }
    }

    fun launchCameraInternal() {
        val imagesDir = File(context.cacheDir, "profile_images")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val file = File(imagesDir, "profile_picture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        tempPhotoUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraInternal()
        } else {
            val activity = context as? ComponentActivity
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
            val activity = context as? ComponentActivity
            if (activity != null && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                showCameraRationaleDialog = true
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val birthDateFormat = remember {
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AwanTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) },
        shape = AwanTheme.shapes.card
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AwanTheme.spacing.xl)
                .padding(bottom = AwanTheme.spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xl)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
                AwanText(
                    text = stringResource(ProfileR.string.profile_edit_title),
                    style = AwanTheme.styles.titleText
                )
                AwanText(
                    text = stringResource(ProfileR.string.profile_section_personal),
                    style = AwanTheme.styles.bodySecondaryText
                )
            }

            // Profile Picture Picker Section with Inline Error
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(AwanTheme.colors.line)
                            .clickable { showPhotoSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        // Priority: Pending (Preview) > Current URL > Placeholder
                        val imageSource = when {
                            pendingPictureUri == "delete" -> null
                            pendingPictureUri != null -> pendingPictureUri
                            else -> profilePictureUrl
                        }

                        if (imageSource != null) {
                            AwanRemoteImage(
                                url = imageSource,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = AwanTheme.colors.textSecondary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Edit Indicator
                    Surface(
                        shape = CircleShape,
                        color = AwanTheme.colors.sky,
                        contentColor = AwanTheme.colors.onSky,
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = 4.dp, y = 4.dp)
                            .clickable { showPhotoSheet = true }
                            .graphicsLayer {
                                shadowElevation = 8f
                                shape = CircleShape
                                clip = false
                            },
                        shadowElevation = 4.dp
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                
                // Inline Error Feedback
                if (error != null) {
                    AwanText(
                        text = error.asString(),
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.destructive),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_first_name),
                            style = AwanTheme.styles.captionText.copy(
                                textStyle = AwanTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            )
                        )
                        AwanTextField(
                            value = firstName,
                            onValueChange = { firstName = it },
                            placeholder = stringResource(ProfileR.string.profile_first_name),
                            modifier = Modifier.fillMaxWidth(),
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)
                    ) {
                        AwanText(
                            text = stringResource(ProfileR.string.profile_last_name),
                            style = AwanTheme.styles.captionText.copy(
                                textStyle = AwanTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            )
                        )
                        AwanTextField(
                            value = lastName,
                            onValueChange = { lastName = it },
                            placeholder = stringResource(ProfileR.string.profile_last_name),
                            modifier = Modifier.fillMaxWidth(),
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xxs)) {
                    AwanText(
                        text = stringResource(ProfileR.string.profile_birth_date),
                        style = AwanTheme.styles.captionText.copy(
                            textStyle = AwanTheme.typography.caption.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        )
                    )
                    AwanCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { showDatePicker = true },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        PreferenceRow(
                            icon = Icons.Default.Cake,
                            title = stringResource(ProfileR.string.profile_birth_date),
                            value = birthDate.ifBlank { stringResource(ProfileR.string.profile_select_date) },
                            onClick = { showDatePicker = true },
                            iconColor = AwanTheme.colors.zoneTangerine
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.sm)
            ) {
                AwanButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    variant = AwanButtonVariant.Quiet
                ) {
                    AwanText(stringResource(ProfileR.string.profile_cancel))
                }
                AwanButton(
                    onClick = { onSave(firstName, lastName, birthDate) },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && firstName.isNotBlank() && lastName.isNotBlank(),
                    isLoading = isLoading,
                    variant = AwanButtonVariant.Primary
                ) {
                    AwanText(text = stringResource(ProfileR.string.profile_save))
                }
            }
        }
    }

    if (showPhotoSheet) {
        ProfilePictureSheet(
            onDismiss = { showPhotoSheet = false },
            onCameraClick = { requestCameraPermissionAndLaunch() },
            onGalleryClick = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            onDeleteClick = if (profilePictureUrl != null || pendingPictureUri != null) onDeletePicture else null
        )
    }

    if (showCameraRationaleDialog) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_camera_permission_rationale_title),
            body = stringResource(ProfileR.string.profile_camera_permission_rationale_message),
            primaryLabel = stringResource(ProfileR.string.profile_grant_permission),
            onPrimary = {
                showCameraRationaleDialog = false
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { showCameraRationaleDialog = false },
            onDismiss = { showCameraRationaleDialog = false }
        )
    }

    if (showCameraSettingsDialog) {
        AwanDialog(
            title = stringResource(ProfileR.string.profile_camera_permission_settings_title),
            body = stringResource(ProfileR.string.profile_camera_permission_settings_message),
            primaryLabel = stringResource(ProfileR.string.profile_open_settings),
            onPrimary = {
                showCameraSettingsDialog = false
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            },
            secondaryLabel = stringResource(ProfileR.string.profile_cancel),
            onSecondary = { showCameraSettingsDialog = false },
            onDismiss = { showCameraSettingsDialog = false }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                LocalDate.parse(birthDate, birthDateFormat)
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli()
            } catch (_: Exception) {
                null
            }
        )
        AwanDatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AwanButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            birthDate = birthDateFormat.format(Instant.ofEpochMilli(millis))
                        }
                        showDatePicker = false
                    },
                    variant = AwanButtonVariant.Quiet
                ) {
                    AwanText(stringResource(ProfileR.string.profile_ok))
                }
            },
            dismissButton = {
                AwanButton(
                    onClick = { showDatePicker = false },
                    variant = AwanButtonVariant.Quiet
                ) {
                    AwanText(stringResource(ProfileR.string.profile_cancel))
                }
            }
        ) {
            AwanDatePicker(state = datePickerState)
        }
    }
}
