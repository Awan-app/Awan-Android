package com.awan.app.notification

import android.util.Log
import com.awan.app.R
import com.awan.app.core.data.sync.SyncWorker
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.devicetoken.repository.DeviceTokenRepository
import com.awan.app.core.domain.notifications.usecase.GetNotificationPreferencesUseCase
import com.awan.app.core.notifications.SessionNotificationPoster
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Push is not how session notifications reach the user.
 *
 * Session reminders are driven on-device from Room by `:core:notifications`. FCM is kept for token
 * registration, server-owned notifications, and schedule invalidation: when MCP or another client
 * changes the server schedule, the push wakes a forced background reconciliation that refreshes
 * Room. The local scheduler then remains the authority for the actual reminder timing.
 */
@AndroidEntryPoint
class AwanFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var deviceTokenRepository: DeviceTokenRepository

    @Inject
    lateinit var authTokenProvider: AuthTokenProvider

    @Inject
    lateinit var notificationPoster: SessionNotificationPoster

    @Inject
    lateinit var getNotificationPreferences: GetNotificationPreferencesUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            deviceTokenRepository.saveLocalFcmToken(token)
            if (authTokenProvider.getAccessToken() != null) {
                deviceTokenRepository.registerDeviceToken(token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data[KEY_TYPE]
        if (type == TYPE_SCHEDULE_CHANGED) {
            // FCM is an invalidation signal, not the schedule payload. Force the existing offline
            // sync so a still-valid 15-minute cache cannot hide an MCP/other-device update.
            SyncWorker.enqueueImmediateSync(applicationContext, forceRefresh = true)
            Log.d(TAG, "Schedule invalidated by push; forced background sync enqueued")
            return
        }

        if (type in LOCALLY_OWNED_TYPES) {
            // The device already schedules these itself. Showing the server's copy as well would
            // double every reminder for as long as the backend keeps sending them.
            Log.d(TAG, "Ignoring push of type $type; sessions are scheduled locally")
            return
        }

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data[KEY_TITLE]
            ?: getString(R.string.app_name)
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data[KEY_BODY]
            ?: return

        // The switch has to be honoured here, not by the server: everything that reaches this
        // point is a reward or the daily wheel, and a "Rewards and daily spin" toggle that still
        // let them through is the muted-the-whole-app failure the per-notification rule exists to
        // prevent. Reading it is a suspend call, so the post moves onto the service's own scope.
        serviceScope.launch {
            if (!getNotificationPreferences().first().rewardsEnabled) {
                Log.d(TAG, "Rewards notifications are switched off; dropping push")
                return@launch
            }
            // Handles the in-app banner when the app is open and the tray notification otherwise.
            notificationPoster.postRemote(title = title, body = body)
        }
    }

    private companion object {
        const val TAG = "AwanNotifications"
        const val KEY_TYPE = "type"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val TYPE_SCHEDULE_CHANGED = "schedule_changed"

        /** Types the on-device scheduler owns; anything else is still the server's to deliver. */
        val LOCALLY_OWNED_TYPES = setOf(
            "session",
            "session_reminder",
            "session_start",
            "session_end",
        )
    }
}
