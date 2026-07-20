package com.awan.feature.auth.impl.domain.model

sealed interface AuthState {
    data object Unauthenticated : AuthState

    data class OtpRequested(val email: String) : AuthState

    data object Authenticated : AuthState

    data object LoggedOut : AuthState
}
