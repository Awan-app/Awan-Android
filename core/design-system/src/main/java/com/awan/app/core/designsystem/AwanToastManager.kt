package com.awan.app.core.designsystem

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class TopToastData(
    val message: String,
    val title: String? = null,
    val icon: ImageVector? = null,
    val durationMs: Long = 4000L,
    val onClick: (() -> Unit)? = null,
    val id: Long = System.currentTimeMillis(),
)

object AwanToastManager {
    private val _currentToast = MutableStateFlow<TopToastData?>(null)
    val currentToast: StateFlow<TopToastData?> = _currentToast.asStateFlow()

    fun showToast(
        message: String,
        title: String? = null,
        icon: ImageVector? = null,
        durationMs: Long = 4000L,
        onClick: (() -> Unit)? = null,
    ) {
        _currentToast.value = TopToastData(
            message = message,
            title = title,
            icon = icon,
            durationMs = durationMs,
            onClick = onClick,
        )
    }

    fun dismissToast() {
        _currentToast.value = null
    }
}
