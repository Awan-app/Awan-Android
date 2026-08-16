package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.ui.components.ZoneTimelineItem

@Composable
fun DailyZonesTimeline(
    zones: List<DailyZone>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        zones.forEachIndexed { index, zone ->
            androidx.compose.runtime.key(zone.id ?: zone.name) {
                ZoneTimelineItem(
                    zone = zone,
                    isLast = index == zones.size - 1,
                    onClick = { /* Read-only */ }
                )
            }
        }
    }
}
