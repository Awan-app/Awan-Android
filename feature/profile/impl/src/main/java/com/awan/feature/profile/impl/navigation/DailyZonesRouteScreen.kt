package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.profile.impl.presentation.DailyZonesAction
import com.awan.feature.profile.impl.presentation.DailyZonesViewModel
import com.awan.feature.profile.impl.ui.DailyZonesScreen

@Composable
fun DailyZonesRouteScreen(
    viewModel: DailyZonesViewModel = hiltViewModel(),
    onRoutineClick: (String?, String?) -> Unit,
    onCreateRoutineClick: (String?, String?) -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onAction(DailyZonesAction.LoadData)
    }

    DailyZonesScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onNavigateToRoutineDetails = onRoutineClick,
        onCreateRoutineClick = { templateId, selectedDate ->
            // If templateId or selectedDate were passed from UI, use them;
            // otherwise fallback to state's selectedDate if needed.
            val targetDate = selectedDate ?: uiState.selectedDate?.toString()
            onCreateRoutineClick(templateId, targetDate)
        },
        onBackClick = onBack,
    )
}