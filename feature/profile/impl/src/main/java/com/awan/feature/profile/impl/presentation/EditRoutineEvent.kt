package com.awan.feature.profile.impl.presentation

sealed interface EditRoutineEvent {
    data object SaveSuccess : EditRoutineEvent
}