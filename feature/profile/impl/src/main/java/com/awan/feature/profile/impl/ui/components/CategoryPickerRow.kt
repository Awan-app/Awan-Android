package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerRow(
    label: String,
    selectedCategoryId: String?,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    onAddCategory: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    val selected = categories.firstOrNull { it.id == selectedCategoryId }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        AwanText(
            text = label,
            style = AwanTheme.styles.bodyText.copy(
                color = AwanTheme.colors.textSecondary,
                textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontSize = 13.sp)
            )
        )
        Box {
            AwanCard(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                AwanText(
                    text = selected?.name ?: stringResource(R.string.profile_zone_category_empty),
                    style = AwanTheme.styles.bodyText
                )
            }

            AwanDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (categories.isEmpty()) {
                    AwanDropdownMenuItem(
                        label = stringResource(R.string.profile_zone_category_empty),
                        onClick = {},
                        enabled = false
                    )
                } else {
                    categories.forEach { category ->
                        val isSelected = category.id == selectedCategoryId
                        AwanDropdownMenuItem(
                            label = category.name,
                            onClick = {
                                expanded = false
                                onCategorySelected(category.id)
                            },
                            selected = isSelected,
                            leading = {
                                AwanChipDot(
                                    tone = AwanChipTone.Lavender,
                                    active = isSelected
                                )
                            }
                        )
                    }
                }
                HorizontalDivider(color = AwanTheme.colors.line)
                AwanDropdownMenuItem(
                    label = stringResource(R.string.profile_category_add),
                    onClick = {
                        expanded = false
                        showAddDialog = true
                    },
                    leading = {
                        Icon(Icons.Default.Add, null, tint = AwanTheme.colors.sky)
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddDialog = false
                newCategoryName = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AwanTheme.colors.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(AwanTheme.colors.line.copy(alpha = 0.6f)),
                )
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                AwanText(
                    text = stringResource(R.string.profile_category_add),
                    style = AwanTheme.typography.heading.copy(
                        fontSize = 19.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = AwanTheme.colors.textPrimary,
                    ),
                )
                AwanTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    placeholder = stringResource(R.string.profile_category_name_placeholder),
                    modifier = Modifier.fillMaxWidth(),
                )
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AwanButton(
                        onClick = {
                            if (newCategoryName.isNotBlank()) {
                                onAddCategory(newCategoryName)
                                showAddDialog = false
                                newCategoryName = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newCategoryName.isNotBlank(),
                    ) {
                        AwanText(
                            text = stringResource(R.string.profile_save),
                            style = AwanTheme.typography.button.copy(
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                    AwanButton(
                        onClick = {
                            showAddDialog = false
                            newCategoryName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        variant = AwanButtonVariant.Secondary,
                    ) {
                        AwanText(
                            text = stringResource(R.string.profile_cancel),
                            style = AwanTheme.typography.button.copy(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
    }
}
