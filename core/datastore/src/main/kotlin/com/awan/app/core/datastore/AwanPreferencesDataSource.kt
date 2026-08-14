package com.awan.app.core.datastore

import androidx.datastore.core.DataStore
import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.datastore.proto.UserPreferences
import com.awan.app.core.datastore.proto.copy
import com.awan.app.core.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AwanPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) : UserPreferencesDataSource {

    override val userPreferences: Flow<UserPreferencesData> = dataStore.data
        .catch { exception ->

            if (exception is IOException) {
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }
        .map { proto -> proto.toData() }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { darkThemeEnabled = enabled } }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { useDynamicColor = enabled } }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.updateData { it.copy { onboardingCompleted = completed } }
    }

    override suspend fun setDefaultZone(zone: String) {
        dataStore.updateData { it.copy { defaultZone = zone } }
    }

    override suspend fun setLocale(locale: String) {
        dataStore.updateData { it.copy { this.locale = locale } }
    }

    override suspend fun setDefaultRegion(region: String) {
        dataStore.updateData { it.copy { defaultRegion = region } }
    }

    override suspend fun setMicPermissionRequested(requested: Boolean) {
        dataStore.updateData { it.copy { micPermissionRequested = requested } }
    }

    override suspend fun setNotificationPreferences(preferences: NotificationPreferences) {
        dataStore.updateData {
            it.copy {
                sessionRemindersDisabled = !preferences.sessionRemindersEnabled
                sessionLiveActivityDisabled = !preferences.sessionLiveActivityEnabled
                sessionEndNotificationDisabled = !preferences.sessionEndEnabled
                sessionFollowUpDisabled = !preferences.sessionFollowUpEnabled
                streakReminderDisabled = !preferences.streakReminderEnabled
                dailyBriefDisabled = !preferences.dailyBriefEnabled
                rewardNotificationsDisabled = !preferences.rewardsEnabled
                sessionReminderLeadMinutes = preferences.reminderLeadMinutes
                sessionSnoozeMinutes = preferences.snoozeMinutes
                sessionFollowUpMinutes = preferences.followUpDelayMinutes
            }
        }
    }

    private fun UserPreferences.toData() = UserPreferencesData(
        darkThemeEnabled = darkThemeEnabled,
        useDynamicColor = useDynamicColor,
        onboardingCompleted = onboardingCompleted,
        defaultZone = defaultZone,
        locale = locale,
        defaultRegion = defaultRegion,
        micPermissionRequested = micPermissionRequested,
        notificationPreferences = toNotificationPreferences(),
    )

    /**
     * The one place the negated proto flags are flipped, and the one place a `0` minute value is
     * read as "never set" rather than as "fire the reminder exactly when the session starts".
     */
    private fun UserPreferences.toNotificationPreferences() = NotificationPreferences(
        sessionRemindersEnabled = !sessionRemindersDisabled,
        sessionLiveActivityEnabled = !sessionLiveActivityDisabled,
        sessionEndEnabled = !sessionEndNotificationDisabled,
        sessionFollowUpEnabled = !sessionFollowUpDisabled,
        streakReminderEnabled = !streakReminderDisabled,
        dailyBriefEnabled = !dailyBriefDisabled,
        rewardsEnabled = !rewardNotificationsDisabled,
        reminderLeadMinutes = sessionReminderLeadMinutes
            .takeIf { it > 0 }
            ?: NotificationPreferences.DEFAULT_REMINDER_LEAD_MINUTES,
        // Not `> 0` like the others: NotificationPreferences.SNOOZE_ASK is negative, and reading it
        // as unset would silently turn "ask me every time" back into a fixed length.
        snoozeMinutes = sessionSnoozeMinutes
            .takeIf { it != 0 }
            ?: NotificationPreferences.DEFAULT_SNOOZE_MINUTES,
        followUpDelayMinutes = sessionFollowUpMinutes
            .takeIf { it > 0 }
            ?: NotificationPreferences.DEFAULT_FOLLOW_UP_MINUTES,
    )
}
