package com.awan.app.core.domain.auth.repository

import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {

    suspend fun requestOtp(email: String): Result<Unit>

    suspend fun verifyOtp(email: String, code: String): Result<AuthSession>

    suspend fun logout(): Result<Unit>

    fun observeIsLoggedIn(): Flow<Boolean>

    fun observeSessionExpired(): Flow<Unit>

    suspend fun getUser(): User?

    suspend fun refreshUserData(): Result<User>
}
