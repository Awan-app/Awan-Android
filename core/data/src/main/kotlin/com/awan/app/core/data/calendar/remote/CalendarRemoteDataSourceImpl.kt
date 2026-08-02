package com.awan.app.core.data.calendar.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.CalendarApiService
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRemoteDataSourceImpl @Inject constructor(
    private val api: CalendarApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : CalendarRemoteDataSource {
    override suspend fun getUserProfile(): Result<UserProfileResponse> =
        safeApiCall(ioDispatcher, json) { api.getUserProfile() }

    override suspend fun getActiveGoals(): Result<List<GoalResponse>> =
        safeApiCall(ioDispatcher, json) {
            buildList {
                var page = 0
                var last = false
                while (!last) {
                    val response = api.getGoals(page = page)
                    addAll(response.content)
                    last = response.last || page + 1 >= response.totalPages
                    page++
                }
            }
        }
}
