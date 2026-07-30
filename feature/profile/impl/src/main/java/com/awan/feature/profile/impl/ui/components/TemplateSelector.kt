package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTextStyle
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.feature.profile.impl.R

@Composable
fun TemplateSelector(
    templates: List<WeeklyTemplate>,
    selectedTemplateId: String?,
    onTemplateSelected: (String) -> Unit,
    onCreateRoutineClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.find { it.id == selectedTemplateId }

    Box(modifier = Modifier.fillMaxWidth()) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .border(1.dp, AwanTheme.colors.line, RoundedCornerShape(12.dp)),
            color = AwanTheme.colors.surface
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(20.dp)
                    )
                    AwanText(
                        text = selectedTemplate?.name ?: stringResource(R.string.profile_routine_select),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Medium)
                        )
                    )
                }
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = AwanTheme.colors.textSecondary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(AwanTheme.colors.surface)
        ) {
            templates.forEach { template ->
                DropdownMenuItem(
                    text = { AwanText(text = template.name) },
                    onClick = {
                        onTemplateSelected(template.id)
                        expanded = false
                    },
                    leadingIcon = {
                        if (template.id == selectedTemplateId) {
                            Icon(Icons.Default.Check, null, tint = AwanTheme.colors.sky)
                        }
                    }
                )
            }
            HorizontalDivider(color = AwanTheme.colors.line, modifier = Modifier.padding(vertical = 4.dp))
            DropdownMenuItem(
                text = {
                    val baseStyle = AwanTheme.styles.bodyText
                    AwanText(
                        text = stringResource(R.string.profile_routine_create_new),
                        style = AwanTextStyle(
                            textStyle = baseStyle.textStyle.copy(fontWeight = FontWeight.Bold),
                            color = AwanTheme.colors.sky
                        )
                    )
                },
                onClick = {
                    onCreateRoutineClick()
                    expanded = false
                }
            )
        }
    }
}
