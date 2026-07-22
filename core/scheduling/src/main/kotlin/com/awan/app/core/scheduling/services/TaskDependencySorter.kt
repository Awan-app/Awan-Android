package com.awan.app.core.scheduling.services

import com.awan.app.core.scheduling.entities.AwanTask
import com.awan.app.core.scheduling.errors.SchedulingException
import java.util.UUID

interface TaskDependencyOrdering {
    fun order(tasks: List<AwanTask>): List<AwanTask>
}

class StableTaskDependencySorter : TaskDependencyOrdering {
    override fun order(tasks: List<AwanTask>): List<AwanTask> {
        val tasksByID = tasks.associateBy { it.id }

        // Detect duplicates
        val duplicateID = tasks.groupBy { it.id }.filter { it.value.size > 1 }.keys.firstOrNull()
        if (duplicateID != null) {
            throw SchedulingException.DuplicateTaskID(duplicateID)
        }

        val knownIDs = tasksByID.keys.toSet()

        // Validate all dependency references exist
        for (task in tasks) {
            val missing = task.dependencyIDs.firstOrNull { it !in knownIDs }
            if (missing != null) {
                throw SchedulingException.MissingDependency(task.id, missing)
            }
        }

        // Kahn's algorithm for topological sort
        val remainingDependencyCount = tasks.associate { it.id to it.dependencyIDs.size }.toMutableMap()
        val dependents = mutableMapOf<UUID, MutableList<UUID>>()
        for (task in tasks) {
            for (depID in task.dependencyIDs) {
                dependents.getOrPut(depID) { mutableListOf() }.add(task.id)
            }
        }

        val eligible = tasks
            .filter { it.dependencyIDs.isEmpty() }
            .sortedBy { it.id.toString() }
            .toMutableList()

        val ordered = mutableListOf<AwanTask>()

        while (eligible.isNotEmpty()) {
            val task = eligible.removeAt(0)
            ordered.add(task)

            for (dependentID in (dependents[task.id] ?: emptyList()).sortedBy { it.toString() }) {
                val count = (remainingDependencyCount[dependentID] ?: 0) - 1
                remainingDependencyCount[dependentID] = count
                if (count == 0) {
                    val dependent = tasksByID[dependentID]
                    if (dependent != null) {
                        eligible.add(dependent)
                        eligible.sortBy { it.id.toString() }
                    }
                }
            }
        }

        if (ordered.size != tasks.size) {
            val orderedIDs = ordered.map { it.id }.toSet()
            throw SchedulingException.DependencyCycle(knownIDs - orderedIDs)
        }

        return ordered
    }
}
