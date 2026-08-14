package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Goal
import com.awan.feature.goals.impl.R
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.X
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditSheet(
    goal: Goal,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String?, status: String, targetDate: String?) -> Unit,
    isSaving: Boolean = false
) {
    var title by remember { mutableStateOf(goal.title) }
    var description by remember { mutableStateOf(goal.description ?: "") }
    var targetDate by remember { mutableStateOf(goal.targetDate) }

    var showDatePicker by remember { mutableStateOf(false) }
    val spacing = AwanTheme.spacing
    val colors = AwanTheme.colors

    if (showDatePicker) {
        AwanDatePickerDialog(
            initialDate = targetDate?.let { LocalDate.parse(it) } ?: LocalDate.now(),
            confirmLabel = stringResource(R.string.goals_date_picker_set),
            cancelLabel = stringResource(R.string.goals_date_picker_cancel),
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                targetDate = date.toString()
                showDatePicker = false
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.md)
                .padding(bottom = spacing.xl),
            verticalArrangement = Arrangement.spacedBy(spacing.md)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = stringResource(R.string.goals_edit_title),
                    style = AwanTheme.typography.title.copy(color = colors.textPrimary)
                )
                AwanIconButton(
                    onClick = onDismiss, 
                    contentDescription = stringResource(R.string.goals_close_content_description)
                ) {
                    Icon(Lucide.X, null, tint = colors.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                AwanText(
                    text = stringResource(R.string.goals_edit_field_title),
                    style = AwanTheme.typography.caption.copy(color = colors.textSecondary)
                )
                AwanTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = stringResource(R.string.goals_edit_placeholder_title)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                AwanText(
                    text = stringResource(R.string.goals_edit_field_description),
                    style = AwanTheme.typography.caption.copy(color = colors.textSecondary)
                )
                AwanTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = stringResource(R.string.goals_edit_placeholder_description),
                    singleLine = false
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
                AwanText(
                    text = stringResource(R.string.goals_edit_field_target_date),
                    style = AwanTheme.typography.caption.copy(color = colors.textSecondary)
                )
                AwanCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(spacing.md)) {
                        AwanText(
                            text = targetDate ?: stringResource(R.string.goals_edit_no_date), 
                            style = AwanTheme.typography.body
                        )
                    }
                }
            }

            AwanButton(
                onClick = {
                    onConfirm(title, description.takeIf { it.isNotBlank() }, goal.status.name, targetDate)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !isSaving,
                isLoading = isSaving
            ) {
                AwanText(text = stringResource(R.string.goals_edit_save))
            }
        }
    }
}
