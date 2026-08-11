package com.awan.app.core.data.devicetoken.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.devicetoken.DeviceTokenResponse
import com.awan.app.core.network.dto.devicetoken.RegisterDeviceTokenRequest

interface DeviceTokenRemoteDataSource {
    suspend fun registerDeviceToken(request: RegisterDeviceTokenRequest): Result<DeviceTokenResponse>
    suspend fun removeDeviceToken(deviceId: String): Result<Unit>
}
