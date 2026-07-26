package com.awan.app.core.designsystem

sealed interface SessionDisplayMode {
    data object Full : SessionDisplayMode
    data object Compact : SessionDisplayMode
    data object Pill : SessionDisplayMode
}
