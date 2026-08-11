package com.awan.app.core.data.devicetoken.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.DeviceTokenApiService
import com.awan.app.core.network.dto.devicetoken.DeviceTokenResponse
import com.awan.app.core.network.dto.devicetoken.RegisterDeviceTokenRequest
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class DeviceTokenRemoteDataSourceImpl @Inject constructor(
    private val deviceTokenApiService: DeviceTokenApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : DeviceTokenRemoteDataSource {

    override suspend fun registerDeviceToken(
        request: RegisterDeviceTokenRequest,
    ): Result<DeviceTokenResponse> = safeApiCall(dispatcher = ioDispatcher, json = json) {
        deviceTokenApiService.registerDeviceToken(request)
    }

    override suspend fun removeDeviceToken(deviceId: String): Result<Unit> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            deviceTokenApiService.removeDeviceToken(deviceId)
        }
}
