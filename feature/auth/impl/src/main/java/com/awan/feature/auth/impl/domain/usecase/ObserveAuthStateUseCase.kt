package com.awan.feature.auth.impl.domain.usecase

import com.awan.feature.auth.impl.data.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository,
) {
    operator fun invoke(): Flow<Boolean> = authRepository.observeIsLoggedIn()
}
