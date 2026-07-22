package com.awan.feature.schedule.impl.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.schedule.api.ScheduleRoute

fun EntryProviderScope<Route>.scheduleEntry() {
    entry<ScheduleRoute> {
        ScheduleRouteScreen()
    }
}

@Composable
fun ScheduleRouteScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Schedule Feature Screen")
        }
    }
}
