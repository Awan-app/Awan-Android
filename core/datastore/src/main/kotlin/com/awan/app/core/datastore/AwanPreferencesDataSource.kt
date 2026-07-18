package com.awan.app.core.datastore

import androidx.datastore.core.DataStore
import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.datastore.proto.UserPreferences
import com.awan.app.core.datastore.proto.copy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AwanPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<UserPreferences>,
) : UserPreferencesDataSource {

    override val userPreferences: Flow<UserPreferencesData> = dataStore.data
        .catch { exception ->

            if (exception is IOException) {
                emit(UserPreferences.getDefaultInstance())
            } else {
                throw exception
            }
        }
        .map { proto -> proto.toData() }

    override suspend fun setDarkThemeEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { darkThemeEnabled = enabled } }
    }

    override suspend fun setDynamicColorEnabled(enabled: Boolean) {
        dataStore.updateData { it.copy { useDynamicColor = enabled } }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.updateData { it.copy { onboardingCompleted = completed } }
    }

    override suspend fun setDefaultZone(zone: String) {
        dataStore.updateData { it.copy { defaultZone = zone } }
    }

    override suspend fun setLocale(locale: String) {
        dataStore.updateData { it.copy { this.locale = locale } }
    }

    override suspend fun setDefaultRegion(region: String) {
        dataStore.updateData { it.copy { defaultRegion = region } }
    }

    private fun UserPreferences.toData() = UserPreferencesData(
        darkThemeEnabled = darkThemeEnabled,
        useDynamicColor = useDynamicColor,
        onboardingCompleted = onboardingCompleted,
        defaultZone = defaultZone,
        locale = locale,
        defaultRegion = defaultRegion,
    )
}
