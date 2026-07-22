package com.awan.app.core.network.device

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

interface DeviceIdProvider {
    fun getDeviceId(): String
}

@Singleton
class AndroidDeviceIdProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : DeviceIdProvider {

    private val prefs by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    override fun getDeviceId(): String {
        val savedId = prefs.getString(KEY_DEVICE_ID, null)
        if (!savedId.isNullOrEmpty()) {
            return savedId
        }

        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    private companion object {
        private const val PREF_NAME = "device_id_prefs"
        private const val KEY_DEVICE_ID = "device_id"
    }
}
