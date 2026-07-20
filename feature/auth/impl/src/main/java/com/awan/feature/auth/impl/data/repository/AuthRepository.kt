package com.awan.feature.auth.impl.data.repository

import com.awan.app.core.common.result.Result
import com.awan.feature.auth.impl.domain.model.AuthSession
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun requestOtp(email: String): Result<Unit>

    suspend fun verifyOtp(email: String, code: String): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    fun observeIsLoggedIn(): Flow<Boolean>
}
