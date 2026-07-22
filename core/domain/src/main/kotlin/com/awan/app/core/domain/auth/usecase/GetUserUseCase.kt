package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class GetUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): User? = authRepository.getUser()
}
