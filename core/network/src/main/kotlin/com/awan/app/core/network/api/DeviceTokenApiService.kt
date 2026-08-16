package com.awan.app.core.network.api

import com.awan.app.core.network.dto.devicetoken.DeviceTokenResponse
import com.awan.app.core.network.dto.devicetoken.RegisterDeviceTokenRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface DeviceTokenApiService {

    @POST("v1/device-tokens")
    suspend fun registerDeviceToken(
        @Body request: RegisterDeviceTokenRequest
    ): DeviceTokenResponse

    @GET("v1/device-tokens")
    suspend fun getUserDevices(): List<DeviceTokenResponse>

    @DELETE("v1/device-tokens/{deviceId}")
    suspend fun removeDeviceToken(
        @Path("deviceId") deviceId: String
    )

    @DELETE("v1/device-tokens")
    suspend fun removeAllDeviceTokens()
}
