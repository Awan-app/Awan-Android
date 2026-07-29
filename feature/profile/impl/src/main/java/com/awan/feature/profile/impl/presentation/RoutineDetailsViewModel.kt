package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.zones.usecase.DeleteWeeklyTemplateUseCase
import com.awan.app.core.domain.zones.usecase.GetWeeklyTemplateUseCase
import com.awan.feature.profile.impl.helpers.DailyZonesHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RoutineDetailsViewModel @Inject constructor(
    private val getWeeklyTemplateUseCase: GetWeeklyTemplateUseCase,
    private val deleteWeeklyTemplateUseCase: DeleteWeeklyTemplateUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(RoutineDetailsState())
    val uiState: StateFlow<RoutineDetailsState> = _uiState.asStateFlow()

    private val _events = Channel<RoutineDetailsEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RoutineDetailsAction) {
        when (action) {
            is RoutineDetailsAction.LoadTemplate -> loadTemplate(action.templateId)
            is RoutineDetailsAction.DeleteRoutine -> deleteRoutine(action.templateId)
        }
    }

    private fun loadTemplate(templateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            when (val result = getWeeklyTemplateUseCase(templateId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isLoading = false, template = result.data) }
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = DailyZonesHelper.zonesErrorToUiText(result.error)
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun deleteRoutine(templateId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, error = null) }
            when (val result = deleteWeeklyTemplateUseCase(templateId)) {
                is Result.Success -> {
                    _uiState.update { it.copy(isDeleting = false) }
                    _events.send(RoutineDetailsEvent.DeleteSuccess)
                }
                is Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            error = DailyZonesHelper.zonesErrorToUiText(result.error)
                        )
                    }
                }
                Result.Loading -> Unit
            }
        }
    }
}
