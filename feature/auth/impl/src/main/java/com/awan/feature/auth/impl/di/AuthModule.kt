package com.awan.feature.auth.impl.di

import com.awan.feature.auth.impl.data.remote.AuthRemoteDataSource
import com.awan.feature.auth.impl.data.remote.AuthRemoteDataSourceImpl
import com.awan.feature.auth.impl.data.repository.AuthRepository
import com.awan.feature.auth.impl.data.repository.AuthRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

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
