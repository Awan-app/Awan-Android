package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfilePartialUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(
        firstName: String? = null,
        lastName: String? = null,
        timezone: String? = null,
        preferredSessionDuration: Int? = null,
        bufferBetweenSessions: Int? = null,
        wakeupTime: String? = null,
        sleepTime: String? = null,
        schedulingType: String? = null
    ): Result<Profile> = repository.updateProfilePartial(
        firstName = firstName,
        lastName = lastName,
        timezone = timezone,
        preferredSessionDuration = preferredSessionDuration,
        bufferBetweenSessions = bufferBetweenSessions,
        wakeupTime = wakeupTime,
        sleepTime = sleepTime,
        schedulingType = schedulingType
    )
}
