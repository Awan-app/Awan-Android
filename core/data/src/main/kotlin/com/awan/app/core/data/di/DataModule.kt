package com.awan.app.core.data.di

import com.awan.app.core.data.onboarding.InMemoryOnboardingRepository
import com.awan.app.core.data.onboarding.OnboardingRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindOnboardingRepository(impl: InMemoryOnboardingRepository): OnboardingRepository
}
