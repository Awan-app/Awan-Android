package com.awan.feature.schedule.impl.di

import com.awan.app.core.scheduling.services.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SchedulingModule {

    @Provides
    @Singleton
    fun provideUUIDGenerator(): UUIDGenerating = SystemUUIDGenerator()

    @Provides
    @Singleton
    fun provideZoneWindowResolver(): ZoneWindowResolving = CalendarZoneWindowResolver()

    @Provides
    @Singleton
    fun provideAvailabilityCalculator(): AvailabilityCalculating = DefaultAvailabilityCalculator()

    @Provides
    @Singleton
    fun provideTaskDependencyOrdering(): TaskDependencyOrdering = StableTaskDependencySorter()

    @Provides
    @Singleton
    fun provideResolutionCandidateGenerator(
        zoneWindowResolver: ZoneWindowResolving,
        availabilityCalculator: AvailabilityCalculating
    ): ResolutionCandidateGenerating = DefaultResolutionCandidateGenerator(
        zoneWindowResolver,
        availabilityCalculator
    )

    @Provides
    @Singleton
    fun provideScheduleEngine(
        dependencyOrdering: TaskDependencyOrdering,
        zoneWindowResolver: ZoneWindowResolving,
        availabilityCalculator: AvailabilityCalculating,
        resolutionCandidateGenerator: ResolutionCandidateGenerating
    ): ScheduleEngine = DefaultScheduleEngine(
        dependencyOrdering,
        zoneWindowResolver,
        availabilityCalculator,
        resolutionCandidateGenerator
    )
}
