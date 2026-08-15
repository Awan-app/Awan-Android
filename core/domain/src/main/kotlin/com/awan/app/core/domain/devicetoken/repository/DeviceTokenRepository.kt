package com.awan.app.core.domain.devicetoken.repository

import com.awan.app.core.common.result.Result

interface DeviceTokenRepository {
    /**
     * Register or update the current device's FCM token with the backend.
     * Fetches current FCM token from Firebase SDK or local storage if provided token is null.
     */
    suspend fun registerDeviceToken(fcmToken: String? = null): Result<Unit>

    /**
     * Remove the current device token from the backend (on logout or session expiration).
     */
    suspend fun removeDeviceToken(): Result<Unit>

    /**
     * Save/cache FCM token locally when refreshed.
     */
    suspend fun saveLocalFcmToken(token: String)

    /**
     * Get saved FCM token locally.
     */
    suspend fun getLocalFcmToken(): String?
}
