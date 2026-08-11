package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.usecase.GetGoalsUseCase
import com.awan.app.core.domain.goal.usecase.ObserveGoalsUseCase
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
class GoalsViewModel @Inject constructor(
    private val getGoalsUseCase: GetGoalsUseCase,
    private val observeGoalsUseCase: ObserveGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    private val _events = Channel<GoalsEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        observeGoals()
        refreshGoals()
    }

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.SearchQueryChanged -> _state.update { it.copy(searchQuery = action.query) }
            GoalsAction.RetryClicked -> refreshGoals()
            is GoalsAction.GoalClicked -> {
                viewModelScope.launch {
                    _events.send(GoalsEvent.NavigateToGoalDetails(action.goalId))
                }
            }

            is GoalsAction.TabSelected -> _state.update { it.copy(tab = action.tab) }
        }
    }

    private fun observeGoals() {
        viewModelScope.launch {
            observeGoalsUseCase().collect { goals ->
                _state.update { it.copy(goals = goals) }
            }
        }
    }

    private fun refreshGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, isError = false) }

            when (val result = getGoalsUseCase()) {
                is Result.Success -> {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isError = false,
                        )
                    }
                }
                is Result.Error -> {
                    _state.update { it.copy(isLoading = false, isError = true) }
                }
                Result.Loading -> {}
            }
        }
    }
}
