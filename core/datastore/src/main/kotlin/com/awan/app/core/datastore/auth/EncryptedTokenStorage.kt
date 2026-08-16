package com.awan.app.core.datastore.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.awan.app.core.common.dispatcher.AwanDispatchers
import com.awan.app.core.common.dispatcher.Dispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedTokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @Dispatcher(AwanDispatchers.IO) private val ioDispatcher: CoroutineDispatcher,
) : AuthTokenProvider {

    private val sharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private val _isLoggedIn by lazy {
        MutableStateFlow(sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false))
    }

    private val _sessionExpired = Channel<Unit>(Channel.CONFLATED)

    override suspend fun getAccessToken(): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
    }

    override suspend fun getRefreshToken(): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(KEY_REFRESH_TOKEN, null)
    }

    override suspend fun saveTokens(accessToken: String, refreshToken: String): Unit =
        withContext(ioDispatcher) {
            sharedPreferences.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply()
        }

    override suspend fun saveUserData(userId: String?, email: String?): Unit =
        withContext(ioDispatcher) {
            sharedPreferences.edit()
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .apply()
        }

    override suspend fun getUserId(): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(KEY_USER_ID, null)
    }

    override suspend fun getUserEmail(): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(KEY_USER_EMAIL, null)
    }

    // KEY_USER_EMAIL deliberately survives so the login screen can pre-fill it.
    override suspend fun clearTokens(): Unit = withContext(ioDispatcher) {
        sharedPreferences.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
        _isLoggedIn.value = false
    }

    override fun observeIsLoggedIn(): Flow<Boolean> = _isLoggedIn.asStateFlow()

    override suspend fun setLoggedIn(loggedIn: Boolean): Unit = withContext(ioDispatcher) {
        sharedPreferences.edit().putBoolean(KEY_IS_LOGGED_IN, loggedIn).apply()
        _isLoggedIn.value = loggedIn
    }

    override val sessionExpired: Flow<Unit> = _sessionExpired.receiveAsFlow()

    // Only a live session can expire. Callers notify before clearTokens(), so concurrent 401s
    // racing through the authenticator collapse to the one signal that found the session alive.
    override fun notifySessionExpired() {
        if (_isLoggedIn.value) _sessionExpired.trySend(Unit)
    }

    override suspend fun saveFcmToken(token: String): Unit = withContext(ioDispatcher) {
        sharedPreferences.edit().putString(KEY_FCM_TOKEN, token).apply()
    }

    override suspend fun getFcmToken(): String? = withContext(ioDispatcher) {
        sharedPreferences.getString(KEY_FCM_TOKEN, null)
    }

    private companion object {
        const val PREFS_FILE_NAME = "awan_auth_tokens_secure"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
        const val KEY_FCM_TOKEN = "fcm_token"
    }
}