package com.awan.feature.aitasks.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.aitasks.api.AiTaskProposalsRoute
import com.awan.feature.aitasks.impl.ui.AiTasksRouteScreen

fun EntryProviderScope<Route>.aiTasksEntry(
    onBack: () -> Unit,
    onTasksCreated: (Int) -> Unit = { onBack() },
) {
    entry<AiTaskProposalsRoute> { route ->
        AiTasksRouteScreen(
            text = route.text,
            note = route.note,
            imageUri = route.imageUri,
            goalId = route.goalId,
            onBack = onBack,
            onTasksCreated = onTasksCreated,
        )
    }
}
