package com.awan.feature.profile.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data object DailyZonesRoute : Route

@Serializable
data class RoutineDetailsRoute(val templateId: String) : Route

@Serializable
data class EditRoutineRoute(val templateId: String? = null) : Route

@Serializable
data class DayDetailsRoute(val date: String) : Route
