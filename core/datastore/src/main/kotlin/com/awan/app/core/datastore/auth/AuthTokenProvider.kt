package com.awan.app.core.datastore.auth

import kotlinx.coroutines.flow.Flow

interface AuthTokenProvider {
    suspend fun getAccessToken(): String?
    suspend fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String)

    suspend fun saveUserData(userId: String?, email: String?)
    suspend fun getUserId(): String?
    suspend fun getUserEmail(): String?

    suspend fun clearTokens()

    fun observeIsLoggedIn(): Flow<Boolean>

    suspend fun setLoggedIn(loggedIn: Boolean)
}

