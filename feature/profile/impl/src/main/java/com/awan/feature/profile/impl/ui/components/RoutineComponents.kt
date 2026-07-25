package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.*
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.app.core.domain.zones.model.WeeklyTemplate
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@Composable
fun TemplateSelector(
    templates: List<WeeklyTemplate>,
    selectedTemplateId: String?,
    onTemplateSelected: (String) -> Unit,
    onCreateRoutineClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedTemplate = templates.find { it.id == selectedTemplateId }
        ?: templates.find { it.name.equals("Default", ignoreCase = true) }

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
                    val baseStyle = AwanTheme.styles.bodyText
                    AwanText(
                        text = selectedTemplate?.name ?: "Select routine",
                        style = AwanTextStyle(
                            textStyle = baseStyle.textStyle.copy(fontWeight = FontWeight.Medium),
                            color = baseStyle.color
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
                .fillMaxWidth(0.9f)
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
                        text = "+ Create new routine",
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

@Composable
fun DaySelector(
    selectedDay: DayOfWeek,
    overriddenDays: Set<DayOfWeek> = emptySet(),
    onDaySelected: (DayOfWeek) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        DayOfWeek.entries.forEach { day ->
            val isSelected = day == selectedDay
            val isToday = DailyZonesHelper.isToday(day)
            val hasOverride = day in overriddenDays

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onDaySelected(day) }
                    .padding(4.dp)
            ) {
                // 3-letter abbreviation label
                AwanText(
                    text = DailyZonesHelper.abbreviation(day),
                    style = AwanTheme.styles.captionText.copy(
                        textStyle = AwanTheme.styles.captionText.textStyle.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) AwanTheme.colors.sky else AwanTheme.colors.textSecondary
                    )
                )
                // Day number circle/square
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) AwanTheme.colors.sky else Color.Transparent)
                        .border(
                            width = if (isToday && !isSelected) 1.dp else 0.dp,
                            color = if (isToday && !isSelected) AwanTheme.colors.sky else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AwanText(
                        text = DailyZonesHelper.abbreviation(day),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else AwanTheme.colors.textPrimary
                        )
                    )
                }
                // Override indicator dot
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                hasOverride && isSelected -> Color.White.copy(alpha = 0.8f)
                                hasOverride -> AwanTheme.colors.sky
                                else -> Color.Transparent
                            }
                        )
                )
            }
        }
    }
}

@Composable
fun RoutineSummaryCard(
    day: DayOfWeek,
    templateName: String,
    zoneCount: Int,
    isOverride: Boolean,
    onResetClick: () -> Unit,
    onEditRoutineClick: (() -> Unit)? = null,
    onCustomizeDayClick: (() -> Unit)? = null
) {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = DailyZonesHelper.displayName(day),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AwanText(
                        text = templateName,
                        style = AwanTheme.styles.bodyText.copy(
                            color = if (isOverride) AwanTheme.colors.sky else AwanTheme.colors.textPrimary
                        )
                    )
                    AwanText(
                        text = "$zoneCount zones",
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                    )
                }

                if (isOverride) {
                    TextButton(onClick = onResetClick) {
                        AwanText(
                            text = "Reset to Default",
                            style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.destructive)
                        )
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        if (onEditRoutineClick != null) {
                            TextButton(onClick = onEditRoutineClick, contentPadding = PaddingValues(0.dp)) {
                                AwanText(
                                    text = "Edit Routine",
                                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky)
                                )
                            }
                        }
                        if (onCustomizeDayClick != null) {
                            TextButton(onClick = onCustomizeDayClick, contentPadding = PaddingValues(0.dp)) {
                                AwanText(
                                    text = "Customize Day",
                                    style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                                )
                            }
                        }
                    }
                }
            }

            // Context banner
            if (isOverride) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AwanTheme.colors.sky.copy(alpha = 0.08f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = AwanTheme.colors.sky,
                            modifier = Modifier.size(13.dp)
                        )
                        AwanText(
                            text = "Editing custom day only — Routine not affected",
                            style = AwanTheme.styles.captionText.copy(
                                color = AwanTheme.colors.sky,
                                textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                            )
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = AwanTheme.colors.line.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = AwanTheme.colors.textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        AwanText(
                            text = "Editing Routine — applies to all days using it",
                            style = AwanTheme.styles.captionText.copy(
                                color = AwanTheme.colors.textSecondary,
                                textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoneDetailItem(zone: DailyZone) {
    val zoneColor = zone.color.toColor()
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(zoneColor.copy(alpha = 0.1f))
            .border(1.dp, zoneColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(zoneColor)
        )
        Column {
            AwanText(
                text = zone.name,
                style = AwanTheme.styles.bodyText.copy(
                    textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                )
            )
            AwanText(
                text = "${DailyZonesHelper.formatTime12h(zone.startTime)} - ${DailyZonesHelper.formatTime12h(zone.endTime)}",
                style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
            )
        }
    }
}
