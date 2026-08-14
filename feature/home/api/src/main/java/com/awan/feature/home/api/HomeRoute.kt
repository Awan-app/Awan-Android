package com.awan.feature.home.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class HomeRoute(val date: String? = null) : Route
