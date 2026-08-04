package com.awan.app.core.data.calendar

import com.awan.app.core.common.result.Result
import com.awan.app.core.data.calendar.local.CalendarLocalDataSource
import com.awan.app.core.data.calendar.remote.CalendarRemoteDataSource
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.domain.calendar.repository.CalendarSnapshot
import com.awan.app.core.network.dto.GoalResponse
import com.awan.app.core.network.dto.UserProfileResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarRepositoryImplTest {
    @Test
    fun refreshKeepsLocalGoalsMissingFromTheResponse() = runTest {
        val local = FakeCalendarLocalDataSource()
        val repository = CalendarRepositoryImpl(
            remote = FakeRemoteDataSource(),
            authTokenProvider = FakeAuthTokenProvider(),
            local = local,
        )

        val result = repository.refresh()

        assertTrue(result is Result.Success)
        assertEquals(setOf("local", "fresh"), local.goalIds)
    }

    private class FakeRemoteDataSource : CalendarRemoteDataSource {
        override suspend fun getUserProfile(): Result<UserProfileResponse> =
            Result.Success(UserProfileResponse(id = "user"))

        override suspend fun getActiveGoals(): Result<List<GoalResponse>> =
            Result.Success(listOf(GoalResponse(id = "fresh", title = "Fresh")))
    }

    private class FakeAuthTokenProvider : AuthTokenProvider {
        override suspend fun getAccessToken(): String? = null
        override suspend fun getRefreshToken(): String? = null
        override suspend fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override suspend fun saveUserData(userId: String?, email: String?) = Unit
        override suspend fun getUserId(): String? = "user"
        override suspend fun getUserEmail(): String? = null
        override suspend fun clearTokens() = Unit
        override fun observeIsLoggedIn(): Flow<Boolean> = emptyFlow()
        override suspend fun setLoggedIn(loggedIn: Boolean) = Unit
        override val sessionExpired: Flow<Unit> = emptyFlow()
        override fun notifySessionExpired() = Unit
    }

    private class FakeCalendarLocalDataSource : CalendarLocalDataSource {
        val goalIds = mutableSetOf("local")

        override fun observeCalendar(userId: String): Flow<CalendarSnapshot?> = emptyFlow()

        override suspend fun upsertCalendar(profile: UserProfileResponse, goals: List<GoalResponse>) {
            goalIds += goals.map(GoalResponse::id)
        }
    }
}
