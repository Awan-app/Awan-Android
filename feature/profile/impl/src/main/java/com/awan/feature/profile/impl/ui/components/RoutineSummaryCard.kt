package com.awan.feature.profile.impl.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.awan.app.core.designsystem.AwanCard
import com.awan.app.core.designsystem.AwanText
import com.awan.app.core.designsystem.AwanTheme
import com.awan.app.core.domain.zones.model.DayOfWeek
import com.awan.feature.profile.impl.R
import com.awan.feature.profile.impl.helpers.DailyZonesHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutineSummaryCard(
    day: DayOfWeek,
    templateName: String,
    zoneCount: Int,
    onEditRoutineClick: (() -> Unit)? = null,
    isOverride: Boolean = false
) {
    AwanCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    AwanText(
                        text = stringResource(DailyZonesHelper.getDayNameRes(day)),
                        style = AwanTheme.styles.bodyText.copy(
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Bold)
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    AwanText(
                        text = templateName,
                        style = AwanTheme.styles.bodyText.copy(
                            color = AwanTheme.colors.textPrimary,
                            textStyle = AwanTheme.styles.bodyText.textStyle.copy(fontWeight = FontWeight.Medium)
                        )
                    )

                    AwanText(
                        text = pluralStringResource(R.plurals.profile_zone_count, zoneCount, zoneCount),
                        style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.textSecondary)
                    )
                }

                if (onEditRoutineClick != null) {
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                        TextButton(
                            onClick = onEditRoutineClick,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.defaultMinSize(minWidth = 1.dp, minHeight = 1.dp)
                        ) {
                            AwanText(
                                text = stringResource(R.string.profile_routine_edit),
                                style = AwanTheme.styles.captionText.copy(color = AwanTheme.colors.sky)
                                    .copy(textStyle = AwanTheme.styles.captionText.textStyle.copy(fontWeight = FontWeight.Bold))
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = (if (isOverride) AwanTheme.colors.sky else AwanTheme.colors.line).copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        if (isOverride) Icons.Default.CalendarToday else Icons.Default.CalendarToday, // Change icon if needed
                        contentDescription = null,
                        tint = if (isOverride) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    AwanText(
                        text = if (isOverride) stringResource(R.string.profile_routine_summary_subtitle_today) 
                               else stringResource(R.string.profile_routine_edit_summary),
                        style = AwanTheme.styles.captionText.copy(
                            color = if (isOverride) AwanTheme.colors.onSky else AwanTheme.colors.textSecondary,
                            textStyle = AwanTheme.styles.captionText.textStyle.copy(fontSize = 10.sp)
                        )
                    )
                }
            }
        }
    }
}
