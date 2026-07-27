package com.awan.app.core.designsystem

sealed interface TaskStatus {
    data object Pending : TaskStatus
    data object Completed : TaskStatus
    data object Fixed : TaskStatus
}
