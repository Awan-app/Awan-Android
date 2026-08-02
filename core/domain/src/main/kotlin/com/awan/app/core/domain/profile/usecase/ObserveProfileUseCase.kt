package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.model.Profile
import com.awan.app.core.domain.profile.repository.ProfileRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveProfileUseCase @Inject constructor(
    private val repository: ProfileRepository
) {
    operator fun invoke(): Flow<Profile?> = repository.observeProfile()
}
