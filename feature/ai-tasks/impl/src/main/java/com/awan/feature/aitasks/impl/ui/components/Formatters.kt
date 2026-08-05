package com.awan.feature.aitasks.impl.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.awan.feature.aitasks.impl.R

const val MinutesPerHour = 60

@Composable
fun durationLabel(minutes: Int): String {
    val hours = minutes / MinutesPerHour
    val remainder = minutes % MinutesPerHour
    return when {
        hours == 0 -> stringResource(R.string.ai_tasks_duration_minutes, remainder)
        remainder == 0 -> stringResource(R.string.ai_tasks_duration_hours, hours)
        else -> stringResource(R.string.ai_tasks_duration_hours_minutes, hours, remainder)
    }
}
