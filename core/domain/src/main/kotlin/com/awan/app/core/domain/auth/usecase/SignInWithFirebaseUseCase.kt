package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.repository.AuthRepository
import javax.inject.Inject

class SignInWithFirebaseUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(idToken: String): Result<AuthSession> =
        authRepository.signInWithFirebase(idToken = idToken)
}
