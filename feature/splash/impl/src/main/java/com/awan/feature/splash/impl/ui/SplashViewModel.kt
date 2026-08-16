package com.awan.feature.splash.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.usecase.ObserveAuthStateUseCase
import com.awan.app.core.domain.goal.usecase.GetPendingScheduleDraftGoalIdUseCase
import com.awan.app.core.domain.onboarding.usecase.HasCompletedOnboardingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Auth : SplashDestination
    data object Onboarding : SplashDestination
    data object Home : SplashDestination
    data class ResumeGoalScheduling(val goalId: String) : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    hasCompletedOnboardingUseCase: HasCompletedOnboardingUseCase,
    getPendingScheduleDraftGoalIdUseCase: GetPendingScheduleDraftGoalIdUseCase,
) : ViewModel() {

    // Auth is checked first so the onboarding lookup never runs signed out.
    val destination: StateFlow<SplashDestination> = observeAuthStateUseCase().map { isLoggedIn ->
        when {
            !isLoggedIn -> SplashDestination.Auth
            !hasCompletedOnboardingUseCase() -> SplashDestination.Onboarding
            else -> when (val pendingGoal = getPendingScheduleDraftGoalIdUseCase()) {
                is Result.Success -> pendingGoal.data?.takeIf { it.isNotBlank() }
                    ?.let(SplashDestination::ResumeGoalScheduling)
                    ?: SplashDestination.Home
                else -> SplashDestination.Home
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SplashDestination.Loading,
    )
}
