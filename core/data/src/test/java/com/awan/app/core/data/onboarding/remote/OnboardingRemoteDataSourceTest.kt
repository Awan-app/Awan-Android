package com.awan.app.core.data.onboarding.remote

import com.awan.app.core.common.result.Result
import com.awan.app.core.network.api.OnboardingApiService
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingRequest
import com.awan.app.core.network.dto.onboarding.CompleteOnboardingResponse
import com.awan.app.core.network.dto.onboarding.OnboardingPreferencesDto
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

class OnboardingRemoteDataSourceTest {

    private val testDispatcher = StandardTestDispatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var mockService: FakeOnboardingApiService
    private lateinit var dataSource: OnboardingRemoteDataSourceImpl

    @Before
    fun setUp() {
        mockService = FakeOnboardingApiService()
        dataSource = OnboardingRemoteDataSourceImpl(
            onboardingApiService = mockService,
            json = json,
            ioDispatcher = testDispatcher,
        )
    }

    @Test
    fun `completeOnboarding returns success result when api service succeeds`() = runTest(testDispatcher.scheduler) {
        val request = CompleteOnboardingRequest(
            firstName = "Abdelrahman",
            lastName = "Emad",
        )
        mockService.responseToReturn = CompleteOnboardingResponse(
            id = "test-user-id",
            email = "user@example.com",
            firstName = "Abdelrahman",
            lastName = "Emad",
            preferences = OnboardingPreferencesDto(timezone = "Africa/Cairo"),
        )

        val result = dataSource.completeOnboarding(request)

        assertTrue(result is Result.Success)
        val successData = (result as Result.Success).data
        assertEquals("test-user-id", successData.id)
        assertEquals("Abdelrahman", successData.firstName)
    }

    @Test
    fun `completeOnboarding returns error result when api service throws exception`() = runTest(testDispatcher.scheduler) {
        val request = CompleteOnboardingRequest(
            firstName = "Test",
            lastName = "User",
        )
        mockService.shouldThrowError = true

        val result = dataSource.completeOnboarding(request)

        assertTrue(result is Result.Error)
    }

    private class FakeOnboardingApiService : OnboardingApiService {
        var responseToReturn: CompleteOnboardingResponse = CompleteOnboardingResponse(id = "default-id")
        var shouldThrowError: Boolean = false

        override suspend fun completeOnboarding(request: CompleteOnboardingRequest): CompleteOnboardingResponse {
            if (shouldThrowError) {
                throw IOException("Network error")
            }
            return responseToReturn
        }
    }
}
