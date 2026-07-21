package com.awan.app.core.domain.auth.model

data class User(
    val id: String?,
    val email: String?,
    val isNew: Boolean = false,
    val accessToken: String? = null,
    val refreshToken: String? = null,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long?,
    val user: User? = null,
)
