package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class GetLastUsedEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): String? = authRepository.getLastUsedEmail()
}
