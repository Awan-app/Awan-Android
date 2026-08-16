package com.awan.feature.taskdetails.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class TaskDetailsRoute(val taskId: String) : Route
