package com.awan.app.core.data.di

import com.awan.app.core.data.auth.remote.AuthRemoteDataSource
import com.awan.app.core.data.auth.remote.AuthRemoteDataSourceImpl
import com.awan.app.core.data.auth.repository.AuthRepositoryImpl
import com.awan.app.core.data.onboarding.OnboardingRepository
import com.awan.app.core.data.onboarding.OnboardingRepositoryImpl
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSource
import com.awan.app.core.data.onboarding.remote.OnboardingRemoteDataSourceImpl
import com.awan.app.core.data.task.TaskRepositoryImpl
import com.awan.app.core.data.task.remote.TaskRemoteDataSource
import com.awan.app.core.data.task.remote.TaskRemoteDataSourceImpl
import com.awan.app.core.data.zone.ZoneRepositoryImpl
import com.awan.app.core.data.zone.remote.ZoneRemoteDataSource
import com.awan.app.core.data.zone.remote.ZoneRemoteDataSourceImpl
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.zone.repository.ZoneRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindOnboardingRemoteDataSource(
        impl: OnboardingRemoteDataSourceImpl,
    ): OnboardingRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindOnboardingRepository(
        impl: OnboardingRepositoryImpl,
    ): OnboardingRepository

    @Binds
    @Singleton
    abstract fun bindTaskRemoteDataSource(
        impl: TaskRemoteDataSourceImpl,
    ): TaskRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTaskRepository(
        impl: TaskRepositoryImpl,
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindZoneRemoteDataSource(
        impl: ZoneRemoteDataSourceImpl,
    ): ZoneRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindZoneRepository(
        impl: ZoneRepositoryImpl,
    ): ZoneRepository

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

    @Binds
    @Singleton
    abstract fun bindGoalRemoteDataSource(
        impl: com.awan.app.core.data.goal.remote.GoalRemoteDataSourceImpl,
    ): com.awan.app.core.data.goal.remote.GoalRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindGoalRepository(
        impl: com.awan.app.core.data.goal.GoalRepositoryImpl,
    ): com.awan.app.core.domain.goal.repository.GoalRepository
}
