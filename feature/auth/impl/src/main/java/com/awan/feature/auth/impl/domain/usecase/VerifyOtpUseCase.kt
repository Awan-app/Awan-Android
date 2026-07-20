package com.awan.feature.auth.impl.domain.usecase

import com.awan.app.core.common.result.Result
import com.awan.feature.auth.impl.data.repository.AuthRepository
import com.awan.feature.auth.impl.domain.model.AuthSession
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String, code: String): Result<AuthSession> =
        authRepository.verifyOtp(email = email, code = code)
}
