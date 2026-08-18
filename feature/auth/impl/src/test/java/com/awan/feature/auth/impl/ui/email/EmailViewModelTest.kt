package com.awan.feature.auth.impl.ui.email

import androidx.lifecycle.SavedStateHandle
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.auth.usecase.GetLastUsedEmailUseCase
import com.awan.app.core.domain.auth.usecase.RequestOtpUseCase
import com.awan.app.core.domain.auth.usecase.SignInWithFirebaseUseCase
import com.awan.feature.auth.impl.ui.google.GoogleSignInHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EmailViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var requestOtpUseCase: RequestOtpUseCase
    private lateinit var getLastUsedEmailUseCase: GetLastUsedEmailUseCase
    private lateinit var signInWithFirebaseUseCase: SignInWithFirebaseUseCase
    private lateinit var googleSignInHelper: GoogleSignInHelper

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        requestOtpUseCase = RequestOtpUseCase(authRepository)
        getLastUsedEmailUseCase = GetLastUsedEmailUseCase(authRepository)
        signInWithFirebaseUseCase = SignInWithFirebaseUseCase(authRepository)
        googleSignInHelper = GoogleSignInHelper()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): EmailViewModel {
        return EmailViewModel(
            savedStateHandle = savedStateHandle,
            requestOtpUseCase = requestOtpUseCase,
            getLastUsedEmailUseCase = getLastUsedEmailUseCase,
            signInWithFirebaseUseCase = signInWithFirebaseUseCase,
            googleSignInHelper = googleSignInHelper,
        )
    }

    @Test
    fun `when email is entered and process dies, restored EmailViewModel retains entered email and validity`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(EmailViewModel.KEY_EMAIL, "user@example.com")
        }

        val viewModel = createViewModel(savedStateHandle)

        assertEquals("user@example.com", viewModel.uiState.value.email)
        assertTrue(viewModel.uiState.value.isEmailValid)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `when onEmailChanged is called, updates SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)

        viewModel.onEmailChanged("new_user@example.com")

        assertEquals("new_user@example.com", savedStateHandle.get<String>(EmailViewModel.KEY_EMAIL))
        assertEquals("new_user@example.com", viewModel.uiState.value.email)
        assertTrue(viewModel.uiState.value.isEmailValid)
    }

    @Test
    fun `when initial SavedStateHandle is empty, falls back to GetLastUsedEmailUseCase`() = runTest {
        authRepository.lastUsedEmail = "persisted_user@example.com"
        val savedStateHandle = SavedStateHandle()

        val viewModel = createViewModel(savedStateHandle)

        assertEquals("persisted_user@example.com", viewModel.uiState.value.email)
        assertTrue(viewModel.uiState.value.isEmailValid)
        assertEquals("persisted_user@example.com", savedStateHandle.get<String>(EmailViewModel.KEY_EMAIL))
    }

    @Test
    fun `when rate limited and process dies, restored EmailViewModel recalculates remaining rate limit cooldown`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(EmailViewModel.KEY_EMAIL, "rate_limited@example.com")
            set(EmailViewModel.KEY_RATE_LIMIT_EMAIL, "rate_limited@example.com")
            set(EmailViewModel.KEY_RATE_LIMIT_TIMESTAMP, System.currentTimeMillis() - 20_000) // 20s ago
        }

        val viewModel = createViewModel(savedStateHandle)

        assertTrue(viewModel.uiState.value.isRateLimited)
        assertTrue(viewModel.uiState.value.rateLimitSecondsRemaining in 38..40)
        assertFalse(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `when rate limit cooldown elapsed during process death, restored EmailViewModel is not rate limited`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(EmailViewModel.KEY_EMAIL, "rate_limited@example.com")
            set(EmailViewModel.KEY_RATE_LIMIT_EMAIL, "rate_limited@example.com")
            set(EmailViewModel.KEY_RATE_LIMIT_TIMESTAMP, System.currentTimeMillis() - 70_000) // 70s ago (> 60s)
        }

        val viewModel = createViewModel(savedStateHandle)

        assertFalse(viewModel.uiState.value.isRateLimited)
        assertEquals(0, viewModel.uiState.value.rateLimitSecondsRemaining)
        assertTrue(viewModel.uiState.value.canSubmit)
    }

    @Test
    fun `when onSendCode succeeds, navigates to Otp and clears rate limit state in SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.onEmailChanged("valid@example.com")

        val events = mutableListOf<EmailEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onSendCode()
        testScheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(EmailEvent.NavigateToOtp("valid@example.com"), events.first())
        assertNull(savedStateHandle.get<String>(EmailViewModel.KEY_RATE_LIMIT_EMAIL))
        assertNull(savedStateHandle.get<Long>(EmailViewModel.KEY_RATE_LIMIT_TIMESTAMP))

        job.cancel()
    }

    @Test
    fun `when onSendCode encounters rate limit, saves rate limit timestamp to SavedStateHandle`() = runTest {
        authRepository.requestOtpResult = Result.Error(
            AppError.Api(
                code = 429,
                body = "Too many requests",
                errorCode = "OTP_RATE_LIMIT_EXCEEDED",
                retryAfterSeconds = 60,
            )
        )
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.onEmailChanged("ratelimit@example.com")

        viewModel.onSendCode()

        assertTrue(viewModel.uiState.value.isRateLimited)
        assertEquals("ratelimit@example.com", savedStateHandle.get<String>(EmailViewModel.KEY_RATE_LIMIT_EMAIL))
        assertTrue(savedStateHandle.contains(EmailViewModel.KEY_RATE_LIMIT_TIMESTAMP))
    }

    private class FakeAuthRepository : AuthRepository {
        var lastUsedEmail: String? = null
        var requestOtpResult: Result<Unit> = Result.Success(Unit)

        override suspend fun requestOtp(email: String): Result<Unit> = requestOtpResult
        override suspend fun verifyOtp(email: String, code: String): Result<AuthSession> = error("Not implemented")
        override suspend fun signInWithFirebase(idToken: String): Result<AuthSession> = error("Not implemented")
        override suspend fun logout(): Result<Unit> = Result.Success(Unit)
        override fun observeIsLoggedIn(): Flow<Boolean> = flowOf(false)
        override fun observeSessionExpired(): Flow<Unit> = MutableSharedFlow()
        override suspend fun getUser(): User? = null
        override suspend fun getLastUsedEmail(): String? = lastUsedEmail
        override suspend fun refreshUserData(): Result<User> = error("Not implemented")
    }
}
