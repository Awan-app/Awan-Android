package com.awan.feature.profile.impl.presentation

sealed interface ProfileEvent {
    data object LogoutSuccess : ProfileEvent
}
