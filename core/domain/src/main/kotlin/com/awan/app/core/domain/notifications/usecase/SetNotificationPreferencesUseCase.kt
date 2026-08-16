package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.domain.profile.repository.UserDataRepository
import javax.inject.Inject

/**
 * Takes the whole object rather than one setter per toggle — six near-identical use cases would say
 * nothing the data class does not already say.
 */
class SetNotificationPreferencesUseCase @Inject constructor(
    private val repository: UserDataRepository,
) {
    suspend operator fun invoke(preferences: NotificationPreferences) =
        repository.setNotificationPreferences(preferences)
}
