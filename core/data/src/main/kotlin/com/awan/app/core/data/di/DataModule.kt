package com.awan.app.core.data.di

import com.awan.app.core.data.auth.remote.AuthRemoteDataSource
import com.awan.app.core.data.auth.remote.AuthRemoteDataSourceImpl
import com.awan.app.core.data.auth.repository.AuthRepositoryImpl
import com.awan.app.core.data.category.CategoryRepositoryImpl
import com.awan.app.core.data.category.remote.CategoryRemoteDataSource
import com.awan.app.core.data.category.remote.CategoryRemoteDataSourceImpl
import com.awan.app.core.data.home.remote.HomeRemoteDataSource
import com.awan.app.core.data.home.remote.HomeRemoteDataSourceImpl
import com.awan.app.core.data.home.repository.HomeRepositoryImpl
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
import com.awan.app.core.domain.category.repository.CategoryRepository
import com.awan.app.core.domain.task.repository.TaskRepository
import com.awan.app.core.domain.zone.repository.ZoneRepository
import com.awan.app.core.data.task.AiTaskRepositoryImpl
import com.awan.app.core.data.template.TemplateRepositoryImpl
import com.awan.app.core.data.template.remote.TemplateRemoteDataSource
import com.awan.app.core.data.template.remote.TemplateRemoteDataSourceImpl
import com.awan.app.core.domain.auth.repository.AuthRepository
import com.awan.app.core.domain.task.repository.AiTaskRepository
import com.awan.app.core.domain.template.repository.TemplateRepository
import com.awan.app.core.domain.home.repository.HomeRepository
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
    abstract fun bindCategoryRemoteDataSource(
        impl: CategoryRemoteDataSourceImpl,
    ): CategoryRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(
        impl: CategoryRepositoryImpl,
    ): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAiTaskRepository(
        impl: AiTaskRepositoryImpl,
    ): AiTaskRepository

    @Binds
    @Singleton
    abstract fun bindTemplateRemoteDataSource(
        impl: TemplateRemoteDataSourceImpl,
    ): TemplateRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindTemplateRepository(
        impl: TemplateRepositoryImpl,
    ): TemplateRepository

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
    abstract fun bindHomeRemoteDataSource(
        impl: HomeRemoteDataSourceImpl,
    ): HomeRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindHomeRepository(
        impl: HomeRepositoryImpl,
    ): HomeRepository
}

