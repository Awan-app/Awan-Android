package com.awan.app.core.database.di

import android.content.Context
import androidx.room.Room
import com.awan.app.core.database.AwanDatabase
import com.awan.app.core.database.dao.CachedScheduleDateDao
import com.awan.app.core.database.dao.CategoryDao
import com.awan.app.core.database.dao.GoalDao
import com.awan.app.core.database.dao.SessionDao
import com.awan.app.core.database.dao.StoreDao
import com.awan.app.core.database.dao.TaskDao
import com.awan.app.core.database.dao.TemplateDao
import com.awan.app.core.database.dao.TemplateOverrideDao
import com.awan.app.core.database.dao.UserDao
import com.awan.app.core.database.dao.ZoneDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module that provides the [AwanDatabase] singleton and all DAOs.
 *
 * DAOs are provided as individual bindings so that feature modules can
 * inject only the DAO they need without depending on the whole database class.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun providesAwanDatabase(
        @ApplicationContext context: Context,
    ): AwanDatabase = Room.databaseBuilder(
        context,
        AwanDatabase::class.java,
        "awan-database",
    )
        .addMigrations(AwanDatabase.MIGRATION_1_2)
        .addMigrations(AwanDatabase.MIGRATION_2_3)
        .addMigrations(AwanDatabase.MIGRATION_3_4)
        .addMigrations(AwanDatabase.MIGRATION_4_5)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()

    @Provides
    fun providesUserDao(database: AwanDatabase): UserDao =
        database.userDao()

    @Provides
    fun providesGoalDao(database: AwanDatabase): GoalDao =
        database.goalDao()

    @Provides
    fun providesTaskDao(database: AwanDatabase): TaskDao =
        database.taskDao()

    @Provides
    fun providesTemplateDao(database: AwanDatabase): TemplateDao =
        database.templateDao()

    @Provides
    fun providesTemplateOverrideDao(database: AwanDatabase): TemplateOverrideDao =
        database.templateOverrideDao()

    @Provides
    fun providesZoneDao(database: AwanDatabase): ZoneDao =
        database.zoneDao()

    @Provides
    fun providesCategoryDao(database: AwanDatabase): CategoryDao =
        database.categoryDao()

    @Provides
    fun providesSessionDao(database: AwanDatabase): SessionDao =
        database.sessionDao()

    @Provides
    fun providesCachedScheduleDateDao(database: AwanDatabase): CachedScheduleDateDao =
        database.cachedScheduleDateDao()

    @Provides
    fun providesStoreDao(database: AwanDatabase): StoreDao =
        database.storeDao()
}
