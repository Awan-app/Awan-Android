package com.awan.feature.splash.impl.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.auth.usecase.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Auth : SplashDestination
    data object Onboarding : SplashDestination
    data object Home : SplashDestination
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    userPreferencesDataSource: UserPreferencesDataSource,
) : ViewModel() {

    val destination: StateFlow<SplashDestination> = combine(
        observeAuthStateUseCase(),
        userPreferencesDataSource.userPreferences,
    ) { isLoggedIn, prefs ->
        when {
            !isLoggedIn -> SplashDestination.Auth
            !prefs.onboardingCompleted -> SplashDestination.Onboarding
            else -> SplashDestination.Home
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SplashDestination.Loading,
    )
}
