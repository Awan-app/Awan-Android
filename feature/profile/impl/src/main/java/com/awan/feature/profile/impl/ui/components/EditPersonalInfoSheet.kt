package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R as ProfileR
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPersonalInfoSheet(
    initialFirstName: String,
    initialLastName: String,
    initialBirthDate: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit,
    isLoading: Boolean = false
) {
    var firstName by remember { mutableStateOf(initialFirstName) }
    var lastName by remember { mutableStateOf(initialLastName) }
    var birthDate by remember { mutableStateOf(initialBirthDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    val birthDateFormat = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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

            Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.lg)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AwanTheme.spacing.md)
                ) {
                    // First Name Field
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)
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

                    // Last Name Field
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)
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

                // Birth Date Field
                Column(verticalArrangement = Arrangement.spacedBy(AwanTheme.spacing.xs)) {
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

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = try {
                birthDateFormat.parse(birthDate)?.time
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
                            birthDate = birthDateFormat.format(Date(millis))
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
