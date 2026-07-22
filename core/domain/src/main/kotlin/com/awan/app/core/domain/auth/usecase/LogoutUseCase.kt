package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
