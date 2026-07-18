package com.awan.app.core.datastore.di

import com.awan.app.core.datastore.AwanPreferencesDataSource
import com.awan.app.core.datastore.UserPreferencesDataSource
import com.awan.app.core.datastore.auth.AuthTokenProvider
import com.awan.app.core.datastore.auth.EncryptedTokenStorage
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceBindingsModule {

    @Binds
    @Singleton
    abstract fun bindUserPreferencesDataSource(
        impl: AwanPreferencesDataSource,
    ): UserPreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindAuthTokenProvider(
        impl: EncryptedTokenStorage,
    ): AuthTokenProvider
}
