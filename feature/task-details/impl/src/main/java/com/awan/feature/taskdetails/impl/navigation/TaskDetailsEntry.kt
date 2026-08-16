package com.awan.feature.taskdetails.impl.navigation

import androidx.navigation3.runtime.EntryProviderScope
import com.awan.core.navigation.Route
import com.awan.feature.taskdetails.api.TaskDetailsRoute
import com.awan.feature.taskdetails.impl.ui.TaskDetailsRoot

fun EntryProviderScope<Route>.taskDetailsEntry(
    onBack: () -> Unit,
) {
    entry<TaskDetailsRoute> { route ->
        TaskDetailsRoot(
            taskId = route.taskId,
            onNavigateBack = onBack,
        )
    }
}
