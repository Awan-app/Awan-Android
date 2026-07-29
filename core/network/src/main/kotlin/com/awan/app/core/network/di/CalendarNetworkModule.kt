package com.awan.app.core.network.di

import com.awan.app.core.network.api.CalendarApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CalendarNetworkModule {
    @Provides
    @Singleton
    fun providesCalendarApiService(retrofit: Retrofit): CalendarApiService =
        retrofit.create(CalendarApiService::class.java)
}
