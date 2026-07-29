package com.awan.app.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

/** Injected rather than read statically so anything that reasons about "now" stays testable. */
@Module
@InstallIn(SingletonComponent::class)
object ClockModule {

    @Provides
    @Singleton
    fun providesClock(): Clock = Clock.systemDefaultZone()
}
