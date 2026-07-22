package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, code: String): Result<AuthSession> =
        authRepository.verifyOtp(email = email, code = code)
}
