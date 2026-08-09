package com.awan.app.core.data.gamification.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.GamificationApiService
import com.awan.app.core.network.dto.gamification.GamificationProgressDto
import com.awan.app.core.network.dto.gamification.WheelConfigDto
import com.awan.app.core.network.dto.gamification.WheelSpinDto
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class GamificationRemoteDataSourceImpl @Inject constructor(
    private val gamificationApiService: GamificationApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : GamificationRemoteDataSource {

    override suspend fun getProgress(): Result<GamificationProgressDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            gamificationApiService.getProgress()
        }

    override suspend fun getActivityDates(
        startDate: String,
        endDate: String,
    ): Result<List<String>> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            gamificationApiService.getActivityDates(startDate = startDate, endDate = endDate)
        }

    override suspend fun getWheelConfig(): Result<WheelConfigDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            gamificationApiService.getWheelConfig()
        }

    override suspend fun spinWheel(): Result<WheelSpinDto> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            gamificationApiService.spinWheel()
        }
}
