package com.awan.feature.profile.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.domain.notifications.usecase.SetNotificationPreferencesUseCase
import com.awan.app.core.model.NotificationPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel @Inject constructor(
    private val getNotificationPreferences: GetNotificationPreferencesUseCase,
    private val setNotificationPreferences: SetNotificationPreferencesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationSettingsState())
    val uiState: StateFlow<NotificationSettingsState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getNotificationPreferences().collect { preferences ->
                _uiState.update { it.copy(preferences = preferences, isLoading = false) }
            }
        }
    }

    fun onAction(action: NotificationSettingsAction) {
        when (action) {
            is NotificationSettingsAction.SetSessionReminders ->
                update { it.copy(sessionRemindersEnabled = action.enabled) }
            is NotificationSettingsAction.SetLiveActivity ->
                update { it.copy(sessionLiveActivityEnabled = action.enabled) }
            is NotificationSettingsAction.SetSessionEnd ->
                update { it.copy(sessionEndEnabled = action.enabled) }
            is NotificationSettingsAction.SetSessionFollowUp ->
                update { it.copy(sessionFollowUpEnabled = action.enabled) }
            is NotificationSettingsAction.SetStreakReminder ->
                update { it.copy(streakReminderEnabled = action.enabled) }
            is NotificationSettingsAction.SetDailyBrief ->
                update { it.copy(dailyBriefEnabled = action.enabled) }
            is NotificationSettingsAction.SetRewards ->
                update { it.copy(rewardsEnabled = action.enabled) }
            is NotificationSettingsAction.SetReminderLead ->
                update { it.copy(reminderLeadMinutes = action.minutes) }
            is NotificationSettingsAction.SetSnooze ->
                update { it.copy(snoozeMinutes = action.minutes) }
            is NotificationSettingsAction.SetFollowUpDelay ->
                update { it.copy(followUpDelayMinutes = action.minutes) }
            is NotificationSettingsAction.SystemPermissionChanged ->
                _uiState.update { it.copy(systemNotificationsEnabled = action.enabled) }
        }
    }

    /**
     * Writes to DataStore only. The scheduler is watching preferences, so it retimes itself — there
     * is no reschedule call to forget here.
     */
    private fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        val updated = transform(_uiState.value.preferences)
        _uiState.update { it.copy(preferences = updated) }
        viewModelScope.launch { setNotificationPreferences(updated) }
    }
}
