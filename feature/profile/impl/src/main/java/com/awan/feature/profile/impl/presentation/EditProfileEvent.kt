package com.awan.feature.profile.impl.presentation

sealed interface EditProfileEvent {
    data object SaveSuccess : EditProfileEvent
}
