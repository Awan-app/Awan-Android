package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAddEditSheet(
    category: Category?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    isSaving: Boolean
) {
    var name by remember { mutableStateOf(category?.name ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AwanTheme.colors.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = AwanTheme.colors.line) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AwanText(
                    text = if (category == null) stringResource(R.string.profile_category_add) else stringResource(R.string.profile_category_edit),
                    style = AwanTheme.styles.titleText
                )
                AwanIconButton(
                    onClick = onDismiss,
                    contentDescription = stringResource(R.string.profile_close)
                ) {
                    Icon(Icons.Default.Close, null, tint = AwanTheme.colors.textSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AwanText(
                    text = stringResource(R.string.profile_routine_name),
                    style = AwanTheme.styles.captionText
                )
                AwanTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.profile_category_name_placeholder),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            AwanButton(
                onClick = { onConfirm(name) },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank() && !isSaving,
                isLoading = isSaving
            ) {
                AwanText(text = stringResource(R.string.profile_save))
            }
        }
    }
}
