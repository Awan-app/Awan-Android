package com.awan.feature.profile.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class EditRoutineRoute(val templateId: String? = null) : Route
