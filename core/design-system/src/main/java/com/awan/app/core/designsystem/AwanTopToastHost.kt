package com.awan.app.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AwanTopToastHost(
    modifier: Modifier = Modifier,
) {
    val toastData by AwanToastManager.currentToast.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        contentAlignment = Alignment.TopCenter,
    ) {
        AnimatedVisibility(
            visible = toastData != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            toastData?.let { data ->
                AwanTopToast(
                    message = data.message,
                    title = data.title,
                    icon = data.icon,
                    durationMs = data.durationMs,
                    onDismiss = { AwanToastManager.dismissToast() },
                    onClick = data.onClick,
                )
            }
        }
    }
}
