package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.style.Style
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.feature.profile.impl.R

@Composable
fun RoutinePicker(
    templates: List<WeeklyTemplate>,
    selectedTemplateId: String?,
    onTemplateSelected: (String) -> Unit,
    onCreateRoutineClick: (String?, String?) -> Unit,
    selectedDate: String? = null,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(vertical = 10.dp)
    ) {
        items(templates) { template ->
            val isSelected = template.id == selectedTemplateId
            val routineColor = template.zones.firstOrNull()?.color?.toColor() ?: AwanTheme.colors.sky
            val colors = AwanTheme.colors
            val chipBg = routineColor.copy(alpha = 0.12f).compositeOver(colors.surface)

            AwanButton(
                onClick = { onTemplateSelected(template.id) },
                variant = AwanButtonVariant.Chip,
                style = if (isSelected) Style {
                    background(chipBg)
                    border(1.dp, routineColor)
                } else Style,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(routineColor)
                    )
                    AwanText(
                        text = template.name,
                        style = AwanTheme.styles.captionText.copy(
                            color = if (isSelected) routineColor else AwanTheme.colors.textPrimary,
                            textStyle = AwanTheme.styles.captionText.textStyle.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        )
                    )
                }
            }
        }

        item {
            AwanButton(
                onClick = { onCreateRoutineClick(null, selectedDate) },
                variant = AwanButtonVariant.Chip,
                modifier = Modifier.wrapContentWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 2.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = AwanTheme.colors.sky,
                        modifier = Modifier.size(14.dp)
                    )
                    AwanText(
                        text = stringResource(R.string.profile_routine_new_chip),
                        style = AwanTheme.styles.captionText.copy(
                            color = AwanTheme.colors.sky,
                            textStyle = AwanTheme.styles.captionText.textStyle.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        )
                    )
                }
            }
        }
    }
}
