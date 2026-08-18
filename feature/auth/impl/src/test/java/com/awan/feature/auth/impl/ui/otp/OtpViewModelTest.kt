package com.awan.feature.auth.impl.ui.otp

import androidx.lifecycle.SavedStateHandle
import com.awan.app.core.common.error.AppError
import com.awan.app.core.common.result.Result
import com.awan.app.core.domain.auth.model.AuthSession
import com.awan.app.core.domain.auth.model.User
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.auth.usecase.RequestOtpUseCase
import com.awan.app.core.domain.auth.usecase.VerifyOtpUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
class OtpViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var authRepository: FakeAuthRepository
    private lateinit var verifyOtpUseCase: VerifyOtpUseCase
    private lateinit var requestOtpUseCase: RequestOtpUseCase

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = FakeAuthRepository()
        verifyOtpUseCase = VerifyOtpUseCase(authRepository)
        requestOtpUseCase = RequestOtpUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()): OtpViewModel {
        return OtpViewModel(
            savedStateHandle = savedStateHandle,
            verifyOtpUseCase = verifyOtpUseCase,
            requestOtpUseCase = requestOtpUseCase,
        )
    }

    @Test
    fun `when process dies on OTP screen, restored OtpViewModel retains email, digits, and status`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(OtpViewModel.KEY_EMAIL, "user@example.com")
            set(OtpViewModel.KEY_DIGITS, arrayListOf("1", "2", "3", "", "", ""))
            set(OtpViewModel.KEY_STATUS, OtpStatus.Idle.name)
            set(OtpViewModel.KEY_SENT_TIMESTAMP, System.currentTimeMillis() - 30_000)
        }

        val viewModel = createViewModel(savedStateHandle)

        assertEquals("user@example.com", viewModel.uiState.value.email)
        assertEquals(listOf("1", "2", "3", "", "", ""), viewModel.uiState.value.digits)
        assertEquals(OtpStatus.Idle, viewModel.uiState.value.status)
        assertTrue(viewModel.uiState.value.resendSecondsRemaining in 88..90)
        assertFalse(viewModel.uiState.value.isResendEnabled)
    }

    @Test
    fun `when resend cooldown elapsed during process death, restored OtpViewModel has resend enabled`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(OtpViewModel.KEY_EMAIL, "user@example.com")
            set(OtpViewModel.KEY_SENT_TIMESTAMP, System.currentTimeMillis() - 130_000) // 130s ago (> 120s)
        }

        val viewModel = createViewModel(savedStateHandle)

        assertTrue(viewModel.uiState.value.isResendEnabled)
        assertEquals(0, viewModel.uiState.value.resendSecondsRemaining)
    }

    @Test
    fun `when setEmail is called with the same email after process death, existing state is not overwritten`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(OtpViewModel.KEY_EMAIL, "user@example.com")
            set(OtpViewModel.KEY_DIGITS, arrayListOf("5", "4", "3", "", "", ""))
            set(OtpViewModel.KEY_SENT_TIMESTAMP, System.currentTimeMillis() - 40_000)
        }

        val viewModel = createViewModel(savedStateHandle)
        // LaunchedEffect in Compose may call setEmail with the route argument
        viewModel.setEmail("user@example.com")

        assertEquals(listOf("5", "4", "3", "", "", ""), viewModel.uiState.value.digits)
        assertTrue(viewModel.uiState.value.resendSecondsRemaining in 78..80)
    }

    @Test
    fun `when setEmail is called with a different email, state is reset`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(OtpViewModel.KEY_EMAIL, "old@example.com")
            set(OtpViewModel.KEY_DIGITS, arrayListOf("1", "2", "3", "", "", ""))
            set(OtpViewModel.KEY_SENT_TIMESTAMP, System.currentTimeMillis() - 40_000)
        }

        val viewModel = createViewModel(savedStateHandle)
        viewModel.setEmail("new@example.com")

        assertEquals("new@example.com", viewModel.uiState.value.email)
        assertEquals(listOf("", "", "", "", "", ""), viewModel.uiState.value.digits)
        assertEquals(120, viewModel.uiState.value.resendSecondsRemaining)
        assertFalse(viewModel.uiState.value.isResendEnabled)
        assertEquals("new@example.com", savedStateHandle.get<String>(OtpViewModel.KEY_EMAIL))
    }

    @Test
    fun `when onDigitsChanged is called, digits are persisted to SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.setEmail("user@example.com")

        viewModel.onDigitsChanged(listOf("1", "2", "3", "4", "", ""))

        assertEquals(listOf("1", "2", "3", "4", "", ""), viewModel.uiState.value.digits)
        assertEquals(
            arrayListOf("1", "2", "3", "4", "", ""),
            savedStateHandle.get<ArrayList<String>>(OtpViewModel.KEY_DIGITS)
        )
    }

    @Test
    fun `when 6 digits entered and verifyOtp succeeds, clears SavedStateHandle and emits NavigateToHome for existing user`() = runTest {
        authRepository.verifyOtpResult = Result.Success(
            AuthSession(
                accessToken = "access_token",
                refreshToken = "refresh_token",
                expiresIn = 3600,
                user = User(
                    id = "user_123",
                    email = "user@example.com",
                    isNew = false,
                )
            )
        )

        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.setEmail("user@example.com")

        val events = mutableListOf<OtpEvent>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.collect { events.add(it) }
        }

        viewModel.onDigitsChanged(listOf("1", "2", "3", "4", "5", "6"))
        testScheduler.advanceUntilIdle()

        assertEquals(1, events.size)
        assertEquals(OtpEvent.NavigateToHome, events.first())
        assertNull(savedStateHandle.get<String>(OtpViewModel.KEY_EMAIL))
        assertNull(savedStateHandle.get<ArrayList<String>>(OtpViewModel.KEY_DIGITS))
        assertNull(savedStateHandle.get<String>(OtpViewModel.KEY_STATUS))
        assertNull(savedStateHandle.get<Long>(OtpViewModel.KEY_SENT_TIMESTAMP))

        job.cancel()
    }

    @Test
    fun `when verifyOtp fails with invalid code, updates status to Wrong and clears digits in SavedStateHandle`() = runTest {
        authRepository.verifyOtpResult = Result.Error(
            AppError.Api(
                code = 400,
                body = "Invalid code",
                errorCode = "OTP_INVALID_CODE",
            )
        )

        val savedStateHandle = SavedStateHandle()
        val viewModel = createViewModel(savedStateHandle)
        viewModel.setEmail("user@example.com")

        viewModel.onDigitsChanged(listOf("9", "9", "9", "9", "9", "9"))

        assertEquals(OtpStatus.Wrong, viewModel.uiState.value.status)
        assertEquals(listOf("", "", "", "", "", ""), viewModel.uiState.value.digits)
        assertEquals(OtpStatus.Wrong.name, savedStateHandle.get<String>(OtpViewModel.KEY_STATUS))
        assertEquals(
            arrayListOf("", "", "", "", "", ""),
            savedStateHandle.get<ArrayList<String>>(OtpViewModel.KEY_DIGITS)
        )
    }

    @Test
    fun `when onResendCode is called, resets digits and timestamp in SavedStateHandle`() = runTest {
        val savedStateHandle = SavedStateHandle().apply {
            set(OtpViewModel.KEY_EMAIL, "user@example.com")
            set(OtpViewModel.KEY_DIGITS, arrayListOf("1", "2", "", "", "", ""))
            set(OtpViewModel.KEY_STATUS, OtpStatus.Expired.name)
            set(OtpViewModel.KEY_SENT_TIMESTAMP, System.currentTimeMillis() - 150_000)
        }

        val viewModel = createViewModel(savedStateHandle)
        viewModel.onResendCode()

        assertEquals(listOf("", "", "", "", "", ""), viewModel.uiState.value.digits)
        assertEquals(OtpStatus.Idle, viewModel.uiState.value.status)
        assertEquals(120, viewModel.uiState.value.resendSecondsRemaining)
        assertFalse(viewModel.uiState.value.isResendEnabled)
        assertTrue(savedStateHandle.contains(OtpViewModel.KEY_SENT_TIMESTAMP))
    }

    private class FakeAuthRepository : AuthRepository {
        var verifyOtpResult: Result<AuthSession>? = null
        var requestOtpResult: Result<Unit> = Result.Success(Unit)

        override suspend fun requestOtp(email: String): Result<Unit> = requestOtpResult
        override suspend fun verifyOtp(email: String, code: String): Result<AuthSession> =
            verifyOtpResult ?: error("verifyOtpResult not set")
        override suspend fun signInWithFirebase(idToken: String): Result<AuthSession> = error("Not implemented")
        override suspend fun logout(): Result<Unit> = Result.Success(Unit)
        override fun observeIsLoggedIn(): Flow<Boolean> = flowOf(false)
        override fun observeSessionExpired(): Flow<Unit> = MutableSharedFlow()
        override suspend fun getUser(): User? = null
        override suspend fun getLastUsedEmail(): String? = null
        override suspend fun refreshUserData(): Result<User> = error("Not implemented")
    }
}
