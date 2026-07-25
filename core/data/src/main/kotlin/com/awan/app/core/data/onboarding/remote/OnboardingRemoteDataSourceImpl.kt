package com.awan.app.core.data.onboarding.remote

import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.OnboardingApiService
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.error.safeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.serialization.json.Json
import javax.inject.Inject

class OnboardingRemoteDataSourceImpl @Inject constructor(
    private val onboardingApiService: OnboardingApiService,
    private val json: Json,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : OnboardingRemoteDataSource {

    override suspend fun completeOnboarding(request: CompleteOnboardingRequest): Result<CompleteOnboardingResponse> =
        safeApiCall(dispatcher = ioDispatcher, json = json) {
            onboardingApiService.completeOnboarding(request)
        }
}
