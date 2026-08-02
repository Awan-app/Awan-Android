package com.awan.feature.calendar.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class CalendarRoute(val date: String? = null) : Route
