package com.awan.app.core.datastore

import com.awan.app.core.datastore.model.UserPreferencesData
import com.awan.app.core.model.NotificationPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesDataSource {
    val userPreferences: Flow<UserPreferencesData>

    suspend fun setDarkThemeEnabled(enabled: Boolean)
    suspend fun setDynamicColorEnabled(enabled: Boolean)
    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setDefaultZone(zone: String)
    suspend fun setLocale(locale: String)
    suspend fun setDefaultRegion(region: String)
    suspend fun setMicPermissionRequested(requested: Boolean)
    suspend fun setNotificationPreferences(preferences: NotificationPreferences)
}
