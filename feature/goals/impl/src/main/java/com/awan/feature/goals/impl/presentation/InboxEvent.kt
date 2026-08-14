package com.awan.feature.goals.impl.presentation

sealed interface InboxEvent {
    data object NavigateBack : InboxEvent
}
