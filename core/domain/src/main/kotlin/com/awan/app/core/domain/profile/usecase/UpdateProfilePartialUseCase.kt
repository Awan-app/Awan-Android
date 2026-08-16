package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import javax.inject.Inject

class UpdateProfilePartialUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    suspend operator fun invoke(firstName: String? = null, lastName: String? = null): Result<Profile> =
        repository.updateProfilePartial(firstName, lastName)
}
