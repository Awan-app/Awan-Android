package com.awan.app.core.data.devicetoken.repository

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.devicetoken.repository.DeviceTokenRepository
import com.awan.app.core.network.api.DeviceTokenApiService
import com.awan.app.core.network.device.DeviceIdProvider
import com.awan.app.core.network.dto.devicetoken.RegisterDeviceTokenRequest
import com.google.android.gms.tasks.Task
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class DeviceTokenRepositoryImpl @Inject constructor(
    private val deviceTokenApiService: DeviceTokenApiService,
    private val deviceIdProvider: DeviceIdProvider,
    private val authTokenProvider: AuthTokenProvider,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : DeviceTokenRepository {

    override suspend fun registerDeviceToken(fcmToken: String?): Result<Unit> = withContext(ioDispatcher) {
        try {
            val token = fcmToken ?: getFcmTokenFromFirebase() ?: authTokenProvider.getFcmToken()
            if (token.isNullOrBlank()) {
                return@withContext Result.Error(AppError.Unknown(IllegalStateException("No FCM token available to register")))
            }

            authTokenProvider.saveFcmToken(token)

            val deviceId = deviceIdProvider.getDeviceId()
            deviceTokenApiService.registerDeviceToken(
                RegisterDeviceTokenRequest(
                    deviceId = deviceId,
                    fcmToken = token,
                    deviceType = "ANDROID",
                )
            )
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun removeDeviceToken(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val deviceId = deviceIdProvider.getDeviceId()
            deviceTokenApiService.removeDeviceToken(deviceId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(AppError.Unknown(e))
        }
    }

    override suspend fun saveLocalFcmToken(token: String): Unit = withContext(ioDispatcher) {
        authTokenProvider.saveFcmToken(token)
    }

    override suspend fun getLocalFcmToken(): String? = withContext(ioDispatcher) {
        authTokenProvider.getFcmToken()
    }

    private suspend fun getFcmTokenFromFirebase(): String? {
        return try {
            FirebaseMessaging.getInstance().token.awaitTask()
        } catch (e: Exception) {
            null
        }
    }
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWith(kotlin.Result.failure(task.exception ?: Exception("Task failed")))
        }
    }
}
