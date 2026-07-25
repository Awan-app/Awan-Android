package com.awan.feature.profile.impl.presentation

sealed interface RoutineDetailsEvent {
    data object DeleteSuccess : RoutineDetailsEvent
}