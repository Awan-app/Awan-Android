package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateSessionSettingsUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(duration: Int, buffer: Int): Result<Profile> =
        repository.updateSessionSettings(duration, buffer)
}
