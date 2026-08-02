package com.awan.app.core.domain.profile.repository

import com.awan.app.core.domain.profile.model.UserData
import kotlinx.coroutines.flow.Flow

interface UserDataRepository {
    val userData: Flow<UserData>
    suspend fun setDarkThemeEnabled(enabled: Boolean)
    suspend fun setLocale(locale: String)
}
