package com.awan.app.core.domain.profile.usecase

import com.awan.app.core.domain.profile.repository.UserDataRepository
import javax.inject.Inject

class SetMicPermissionRequestedUseCase @Inject constructor(
    private val userDataRepository: UserDataRepository,
) {
    suspend operator fun invoke(requested: Boolean) {
        userDataRepository.setMicPermissionRequested(requested)
    }
}
