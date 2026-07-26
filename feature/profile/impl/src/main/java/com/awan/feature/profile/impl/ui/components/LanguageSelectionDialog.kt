package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.*
import com.awan.feature.profile.impl.R

@Composable
fun LanguageSelectionDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { AwanText(text = stringResource(R.string.profile_select_language), style = AwanTheme.styles.titleText) },
        text = {
            Column {
                LanguageOption(
                    label = stringResource(R.string.profile_language_english),
                    code = "en",
                    isSelected = currentLanguage == "en" || currentLanguage == "",
                    onSelect = onLanguageSelected
                )
                LanguageOption(
                    label = stringResource(R.string.profile_language_arabic),
                    code = "ar",
                    isSelected = currentLanguage == "ar",
                    onSelect = onLanguageSelected
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                AwanText(text = stringResource(R.string.profile_cancel), style = AwanTheme.styles.buttonCompactText)
            }
        },
        containerColor = AwanTheme.colors.surface,
        shape = AwanTheme.shapes.card
    )
}

@Composable
private fun LanguageOption(
    label: String,
    code: String,
    isSelected: Boolean,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) AwanTheme.colors.background else Color.Transparent)
            .clickable { onSelect(code) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RadioButton(selected = isSelected, onClick = { onSelect(code) })
        AwanText(text = label, style = AwanTheme.styles.bodyText)
    }
}
