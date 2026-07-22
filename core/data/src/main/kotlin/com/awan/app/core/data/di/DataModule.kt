package com.awan.app.core.data.di

import com.awan.app.core.data.auth.remote.AuthRemoteDataSource
import com.awan.app.core.data.auth.remote.AuthRemoteDataSourceImpl
import com.awan.app.core.data.auth.repository.AuthRepositoryImpl
import com.awan.app.core.data.onboarding.InMemoryOnboardingRepository
import com.awan.app.core.data.onboarding.OnboardingRepository
import com.awan.app.core.domain.auth.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    abstract fun bindOnboardingRepository(impl: InMemoryOnboardingRepository): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(
        impl: AuthRemoteDataSourceImpl,
    ): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl,
    ): AuthRepository
}
