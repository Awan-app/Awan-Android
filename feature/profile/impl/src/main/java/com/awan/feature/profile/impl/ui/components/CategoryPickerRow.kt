package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.model.Category
import com.awan.feature.profile.impl.R

@Composable
fun CategoryPickerRow(
    label: String,
    selectedCategoryId: String?,
    categories: List<Category>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
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
            }
        }
    }
}
