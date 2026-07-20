package com.awan.feature.auth.impl.ui.email

import androidx.compose.runtime.Immutable

@Immutable
data class EmailUiState(
    val email: String = "",
    val isEmailValid: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRateLimited: Boolean = false,
    val rateLimitSecondsRemaining: Int = 0,
    val isOffline: Boolean = false,
)

val EmailUiState.canSubmit: Boolean
    get() = isEmailValid && !isLoading && !isRateLimited
