package com.awan.app.core.data.profile.repository

import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.domain.profile.model.UserData
import com.awan.app.core.domain.profile.repository.UserDataRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserDataRepositoryImpl @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : UserDataRepository {
    override val userData: Flow<UserData> = userPreferencesDataSource.userPreferences.map {
        UserData(
            darkThemeEnabled = it.darkThemeEnabled,
            locale = it.locale
        )
    }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        userPreferencesDataSource.setDarkThemeEnabled(enabled)
    }

    override suspend fun setLocale(locale: String) {
        userPreferencesDataSource.setLocale(locale)
    }
}
