package com.awan.feature.profile.impl.navigation

import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.awan.feature.profile.impl.presentation.NotificationSettingsAction
import com.awan.feature.profile.impl.presentation.NotificationSettingsViewModel
import com.awan.feature.profile.impl.ui.NotificationSettingsScreen

@Composable
fun NotificationSettingsRouteScreen(
    viewModel: NotificationSettingsViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Re-read on every resume: the user leaves for system settings, flips the permission, and comes
    // back — nothing tells us about that but the return.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onAction(
            NotificationSettingsAction.SystemPermissionChanged(
                NotificationManagerCompat.from(context).areNotificationsEnabled()
            )
        )
    }

    NotificationSettingsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onBackClick = onBack,
        onOpenSystemSettings = {
            // Opens this app's notification page directly, where the per-channel controls live.
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        },
    )
}
