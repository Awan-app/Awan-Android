package com.awan.app.core.domain.notifications.usecase

import com.awan.app.core.model.NotificationPreferences
import com.awan.app.core.domain.profile.repository.UserDataRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetNotificationPreferencesUseCase @Inject constructor(
    private val repository: UserDataRepository,
) {
    operator fun invoke(): Flow<NotificationPreferences> =
        repository.userData.map { it.notificationPreferences }
}
