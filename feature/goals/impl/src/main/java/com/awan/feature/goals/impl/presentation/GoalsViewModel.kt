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

import com.awan.app.core.domain.goal.usecase.ObserveGoalsUseCase
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class GoalsViewModel @Inject constructor(
    private val observeGoalsUseCase: ObserveGoalsUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(GoalsState())
    val state: StateFlow<GoalsState> = _state.asStateFlow()

    init {
        observeGoalsUseCase()
            .onEach { goals ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        isError = false,
                        activeGoals = goals.filter { goal -> !goal.isCompleted },
                        completedGoals = goals.filter { goal -> goal.isCompleted },
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    fun onAction(action: GoalsAction) {
        when (action) {
            is GoalsAction.TabSelected -> _state.update { it.copy(tab = action.tab) }
            GoalsAction.RetryClicked -> { /* No-op in reactive mode, sync would handle this */ }
        }
    }
}
