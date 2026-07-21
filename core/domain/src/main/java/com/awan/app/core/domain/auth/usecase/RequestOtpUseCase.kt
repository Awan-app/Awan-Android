package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class RequestOtpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(email: String): Result<Unit> =
        authRepository.requestOtp(email)
}
