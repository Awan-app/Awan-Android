package com.awan.app.core.domain.auth.usecase

import com.awan.app.core.domain.auth.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeIsLoggedIn()
}
