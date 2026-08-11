package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.usecase.GetGoalUseCase
import com.awan.app.core.model.Goal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalDetailsViewModel @Inject constructor(
    private val getGoalUseCase: GetGoalUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalDetailsState())
    val state: StateFlow<GoalDetailsState> = _state.asStateFlow()

    fun loadGoal(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = getGoalUseCase(id)) {
                is Result.Success<*> -> {
                    val goal = result.data as? Goal
                    _state.update { it.copy(isLoading = false, goal = goal) }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, error = "Failed to load goal") }
                }
                Result.Loading -> {}
            }
        }
    }

    fun onAction(action: GoalDetailsAction) {
        when (action) {
            GoalDetailsAction.Retry -> {
                _state.value.goal?.id?.let { loadGoal(it) }
            }

            GoalDetailsAction.Back -> {}
        }
    }
}
