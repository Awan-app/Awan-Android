package com.awan.feature.goals.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Goal
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
    var status by remember { mutableStateOf(goal.status.name) }
    var targetDate by remember { mutableStateOf(goal.targetDate) }

    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        AwanDatePickerDialog(
            initialDate = targetDate?.let { LocalDate.parse(it) } ?: LocalDate.now(),
            confirmLabel = "Set",
            cancelLabel = "Cancel",
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
        containerColor = AwanTheme.colors.surface,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = "Edit Goal",
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(onClick = onDismiss, contentDescription = "Close") {
                    Icon(Lucide.X, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = "Title",
                    style = AwanTheme.typography.caption.copy(color = AwanTheme.colors.textSecondary)
                )
                AwanTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Enter goal title"
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = "Description",
                    style = AwanTheme.typography.caption.copy(color = AwanTheme.colors.textSecondary)
                )
                AwanTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = "Enter goal description",
                    singleLine = false
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = "Status",
                    style = AwanTheme.typography.caption.copy(color = AwanTheme.colors.textSecondary)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val statuses = listOf("ACTIVE", "ACHIEVED")
                    statuses.forEach { s ->
                        FilterChip(
                            selected = status == s,
                            onClick = { status = s },
                            label = { 
                                AwanText(
                                    text = s, 
                                    style = AwanTheme.typography.caption.copy(
                                        color = if (status == s) AwanTheme.colors.onSky else AwanTheme.colors.textPrimary
                                    )
                                ) 
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AwanTheme.colors.sky,
                                selectedLabelColor = AwanTheme.colors.onSky
                            )
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AwanText(
                    text = "Target Date",
                    style = AwanTheme.typography.caption.copy(color = AwanTheme.colors.textSecondary)
                )
                AwanCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp)) {
                        AwanText(text = targetDate ?: "No date set", style = AwanTheme.typography.body)
                    }
                }
            }

            AwanButton(
                onClick = {
                    onConfirm(title, description.takeIf { it.isNotBlank() }, status, targetDate)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = title.isNotBlank() && !isSaving,
                isLoading = isSaving
            ) {
                AwanText(text = "Save Changes")
            }
        }
    }
}
