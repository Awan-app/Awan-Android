package com.awan.feature.profile.impl.ui.dailyzones

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.awan.app.core.domain.zones.model.DailyZone
import com.awan.feature.profile.impl.ui.components.ZoneTimelineItem

@Composable
fun DailyZonesTimeline(
    zones: List<DailyZone>,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(
            items = zones,
            key = { _, zone -> zone.id ?: zone.name }
        ) { index, zone ->
            ZoneTimelineItem(
                zone = zone,
                isLast = index == zones.size - 1,
                onClick = { /* Read-only */ }
            )
        }
    }
}
