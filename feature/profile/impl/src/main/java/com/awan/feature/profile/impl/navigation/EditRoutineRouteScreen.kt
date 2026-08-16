package com.awan.feature.profile.impl.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.awan.feature.profile.impl.presentation.EditRoutineAction
import com.awan.feature.profile.impl.presentation.EditRoutineEvent
import com.awan.feature.profile.impl.presentation.EditRoutineViewModel
import com.awan.feature.profile.impl.ui.EditRoutineScreen

@Composable
fun EditRoutineRouteScreen(
    templateId: String?,
    overrideId: String?,
    date: String?,
    viewModel: EditRoutineViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId, overrideId, date) {
        viewModel.onAction(EditRoutineAction.LoadTemplate(templateId, overrideId, date))
    }

    LaunchedEffect(viewModel.events) {
        viewModel.events.collect { event ->
            when (event) {
                EditRoutineEvent.SaveSuccess -> onBack()
                EditRoutineEvent.DeleteSuccess -> onBack()
            }
        }
    }

    EditRoutineScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack
    )
}
