package com.awan.app.core.datastore

import androidx.datastore.core.DataStore
import com.awan.app.core.datastore.proto.UserPreferences
import com.awan.app.core.datastore.proto.copy
import com.awan.app.core.model.DarkThemeConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AwanPreferencesDataSourceTest {

    private val inMemoryDataStore = InMemoryDataStore(UserPreferences.getDefaultInstance())
    private val dataSource = AwanPreferencesDataSource(inMemoryDataStore)

    @Test
    fun `default preferences map darkThemeConfig to FOLLOW_SYSTEM`() = runTest {
        val prefs = dataSource.userPreferences.first()
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, prefs.darkThemeConfig)
    }

    @Test
    fun `completing onboarding without setting theme preserves FOLLOW_SYSTEM`() = runTest {
        dataSource.setOnboardingCompleted(true)

        val prefs = dataSource.userPreferences.first()
        assertTrue(prefs.onboardingCompleted)
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, prefs.darkThemeConfig)
    }

    @Test
    fun `legacy darkThemeEnabled true maps to DARK when darkThemeConfig is unset`() = runTest {
        inMemoryDataStore.updateData { it.copy { darkThemeEnabled = true } }

        val prefs = dataSource.userPreferences.first()
        assertEquals(DarkThemeConfig.DARK, prefs.darkThemeConfig)
    }

    @Test
    fun `setting explicit theme config updates darkThemeConfig correctly`() = runTest {
        dataSource.setDarkThemeConfig(DarkThemeConfig.DARK)
        assertEquals(DarkThemeConfig.DARK, dataSource.userPreferences.first().darkThemeConfig)

        dataSource.setDarkThemeConfig(DarkThemeConfig.LIGHT)
        assertEquals(DarkThemeConfig.LIGHT, dataSource.userPreferences.first().darkThemeConfig)

        dataSource.setDarkThemeConfig(DarkThemeConfig.FOLLOW_SYSTEM)
        assertEquals(DarkThemeConfig.FOLLOW_SYSTEM, dataSource.userPreferences.first().darkThemeConfig)
    }

    @Test
    fun `explicit theme config takes precedence over legacy darkThemeEnabled`() = runTest {
        inMemoryDataStore.updateData { it.copy { darkThemeEnabled = true } }
        dataSource.setDarkThemeConfig(DarkThemeConfig.LIGHT)

        val prefs = dataSource.userPreferences.first()
        assertEquals(DarkThemeConfig.LIGHT, prefs.darkThemeConfig)
    }

    private class InMemoryDataStore<T>(initialValue: T) : DataStore<T> {
        private val _data = MutableStateFlow(initialValue)
        override val data: Flow<T> = _data

        override suspend fun updateData(transform: suspend (t: T) -> T): T {
            val updated = transform(_data.value)
            _data.value = updated
            return updated
        }
    }
}
