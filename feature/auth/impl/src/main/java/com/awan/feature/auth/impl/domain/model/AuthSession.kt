package com.awan.feature.auth.impl.domain.model

data class User(
    val id: String?,
    val email: String?,
    val isNew: Boolean,
)

data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long?,
    val user: User? = null,
)
