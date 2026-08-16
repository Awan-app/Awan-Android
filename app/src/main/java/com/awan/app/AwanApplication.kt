package com.awan.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.awan.app.core.notifications.SessionNotificationStarter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class AwanApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sessionNotificationStarter: SessionNotificationStarter

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        // Runs in every process that starts the app, including one woken by WorkManager — which is
        // what keeps alarms current after a background sync rewrites the schedule.
        sessionNotificationStarter.start()
    }
}
