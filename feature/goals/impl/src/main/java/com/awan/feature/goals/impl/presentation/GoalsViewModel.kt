package com.awan.feature.goals.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.goal.repository.GoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    init {
        loadGoals()
    }

    private fun loadGoals() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            
            when (val result = goalRepository.getGoals()) {
                is Result.Success -> {
                    val goals = result.data
                    _state.update {
                        it.copy(
                            isLoading = false,
                            activeGoals = goals.filter { !it.isCompleted },
                            completedGoals = goals.filter { it.isCompleted },
                        )
                    }
                }
                is Result.Error -> {
                    // For now, just stop loading. Future: show error message
                    _state.update { it.copy(isLoading = false) }
                }
                is Result.Loading -> {
                    // Not emitted by repository, but handled for exhaustiveness
                }
            }
        }
    }

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.TabSelected -> _state.update { it.copy(tab = action.tab) }
        }
    }
}
