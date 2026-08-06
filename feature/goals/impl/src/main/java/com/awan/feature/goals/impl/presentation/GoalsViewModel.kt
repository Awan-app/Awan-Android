package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val getGoalsUseCase: GetGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    init {
        loadGoals()
    }

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.TabSelected -> _state.update { it.copy(tab = action.tab) }
            GoalsAction.RetryClicked -> loadGoals()
        }
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }

            when (val result = getGoalsUseCase()) {
                is Result.Success -> {
                    val goals = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                            activeGoals = goals.filter { goal -> !goal.isCompleted },
                            completedGoals = goals.filter { goal -> goal.isCompleted },
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, isError = true) }
                }
                Result.Loading -> {
                    // Handled before invoke
                }
            }
        }
    }
}
