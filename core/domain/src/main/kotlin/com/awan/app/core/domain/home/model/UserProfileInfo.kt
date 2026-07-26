package com.awan.app.core.domain.home.model

data class UserProfileInfo(
    val id: String,
    val firstName: String,
    val lastName: String,
    val points: Int,
    val streak: Int,
)
