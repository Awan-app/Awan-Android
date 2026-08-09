package com.awan.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.auth.usecase.ObserveSessionExpiredUseCase
import com.awan.app.core.domain.gamification.model.RewardEvent
import com.awan.app.core.domain.gamification.usecase.ObserveRewardEventsUseCase
import com.awan.app.core.domain.network.NetworkConnectivityMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserPreferencesDataSource,
    observeSessionExpired: ObserveSessionExpiredUseCase,
    observeRewardEvents: ObserveRewardEventsUseCase,
    connectivityMonitor: NetworkConnectivityMonitor,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            initialValue = connectivityMonitor.isCurrentlyOnline(),
            started = SharingStarted.WhileSubscribed(5_000),
        )

    val sessionExpired: Flow<Unit> = observeSessionExpired()

    /** Hoisted to the shell so a reward earned on one screen still celebrates on another. */
    val rewardEvents: Flow<RewardEvent> = observeRewardEvents()

    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userPreferences
        .map {
            MainActivityUiState.Success(
                useDarkTheme = it.darkThemeEnabled,
                language = it.locale
            )
        }
        .stateIn(
            scope = viewModelScope,
            initialValue = MainActivityUiState.Loading,
            started = SharingStarted.WhileSubscribed(5_000),
        )
}

sealed interface MainActivityUiState {
    data object Loading : MainActivityUiState
    data class Success(
        val useDarkTheme: Boolean,
        val language: String
    ) : MainActivityUiState
}
