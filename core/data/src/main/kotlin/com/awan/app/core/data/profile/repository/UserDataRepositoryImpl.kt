package com.awan.app.core.data.profile.repository

import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.repository.UserDataRepository
import com.awan.app.core.model.DarkThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserDataRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : UserDataRepository {
    override val userData: Flow<UserData> = userPreferencesDataSource.userPreferences.map {
        UserData(
            darkThemeConfig = it.darkThemeConfig,
            locale = it.locale,
            micPermissionRequested = it.micPermissionRequested,
        )
    }

    override suspend fun setDarkThemeConfig(config: DarkThemeConfig) {
        userPreferencesDataSource.setDarkThemeConfig(config)
    }

    override suspend fun setLocale(locale: String) {
        userPreferencesDataSource.setLocale(locale)
    }

    override suspend fun setMicPermissionRequested(requested: Boolean) {
        userPreferencesDataSource.setMicPermissionRequested(requested)
    }
}
