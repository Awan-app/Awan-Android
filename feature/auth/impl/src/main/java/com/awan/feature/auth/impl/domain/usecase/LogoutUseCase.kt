package com.awan.feature.auth.impl.domain.usecase

import com.awan.app.core.common.result.Result
import com.awan.feature.auth.impl.data.repository.AuthRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): Result<Unit> = authRepository.logout()
}
