package com.awan.app

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.awan.app.core.data.sync.SyncWorker
import com.awan.app.core.notifications.NotificationMaintenanceScheduler
import com.awan.app.core.notifications.SessionNotificationStarter
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class AwanApplication : Application(), Configuration.Provider, SingletonImageLoader.Factory {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var sessionNotificationStarter: SessionNotificationStarter

    @Inject
    lateinit var notificationMaintenanceScheduler: NotificationMaintenanceScheduler

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory(callFactory = okHttpClient))
            }
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // Runs in every process that starts the app, including one woken by WorkManager — which is
        // what keeps alarms current after a background sync rewrites the schedule.
        sessionNotificationStarter.start()

        // Event alarms are allowed to disappear when there is nothing left in today's plan. Keep an
        // independent next-day wake-up so that an empty day cannot strand daily notifications until
        // the user opens Awan again.
        notificationMaintenanceScheduler.scheduleNext()

        // Recovery path for server-side changes whose FCM invalidation is delayed or dropped. The
        // work is unique/KEEP, so calling this on every process start is idempotent.
        SyncWorker.schedulePeriodicSync(this)
    }
}
