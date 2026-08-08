package com.awan.app.core.data.gamification.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.dto.gamification.GamificationProgressDto
import com.awan.app.core.network.dto.gamification.WheelConfigDto
import com.awan.app.core.network.dto.gamification.WheelSpinDto

interface GamificationRemoteDataSource {

    suspend fun getProgress(): Result<GamificationProgressDto>

    suspend fun getActivityDates(startDate: String, endDate: String): Result<List<String>>

    suspend fun getWheelConfig(): Result<WheelConfigDto>

    suspend fun spinWheel(): Result<WheelSpinDto>
}
