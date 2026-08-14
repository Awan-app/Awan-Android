package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.awan.app.core.designsystem.AwanButton
import com.awan.app.core.designsystem.AwanText
import com.awan.feature.profile.impl.R

@Composable
fun DailyZonesBottomActions(
    hasTemplate: Boolean,
    hasOverride: Boolean = false,
    isSaving: Boolean,
    onAddZoneClick: () -> Unit,
    onCreateRoutineClick: (String?, String?) -> Unit,
    selectedDate: String? = null,
) {
    Column(
        modifier = Modifier.padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (hasTemplate || hasOverride) {
            AwanButton(
                onClick = onAddZoneClick,
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Default.Add,
                enabled = !isSaving
            ) {
                AwanText(text = stringResource(R.string.profile_daily_zones_add_zone))
            }
        } else {
            AwanButton(
                onClick = { onCreateRoutineClick(null, selectedDate) },
                modifier = Modifier.fillMaxWidth(),
                icon = { Icon(Icons.Default.Schedule, contentDescription = null) }
            ) {
                AwanText(text = stringResource(R.string.profile_routine_create_for_day))
            }
        }
    }
}