package com.awan.app.core.scheduling.errors

import java.util.UUID

sealed class SchedulingException(message: String) : Exception(message) {
    class InvalidLocalTime(hour: Int, minute: Int) : 
        SchedulingException("Invalid local time: $hour:$minute")
    
    class InvalidDuration(minutes: Int) : 
        SchedulingException("Invalid duration: $minutes minutes")
    
    object InvalidTimeRange : 
        SchedulingException("Invalid time range: start must be before end")
    
    class InvalidColorHex(hex: String) : 
        SchedulingException("Invalid color hex: $hex")
    
    object InvalidConfiguration : 
        SchedulingException("Invalid scheduling configuration")
    
    class DuplicateTaskID(id: UUID) : 
        SchedulingException("Duplicate task ID: $id")
    
    class MissingZone(taskID: UUID, zoneID: UUID) : 
        SchedulingException("Task $taskID is missing zone $zoneID")
    
    class MissingDependency(taskID: UUID, dependencyID: UUID) : 
        SchedulingException("Task $taskID is missing dependency $dependencyID")
    
    class DependencyCycle(taskIDs: Set<UUID>) : 
        SchedulingException("Dependency cycle detected among tasks: $taskIDs")
    
    class EntityNotFound(id: UUID) : 
        SchedulingException("Entity not found: $id")
    
    object InvalidScenarioState : 
        SchedulingException("Invalid scenario state")
}
