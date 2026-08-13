package com.awan.feature.home.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

/**
 * [sessionId] opens that session's detail sheet on arrival — how a notification tap lands on the
 * thing it was about instead of just on the home screen.
 */
@Serializable
data class HomeRoute(
    val date: String? = null,
    val sessionId: String? = null,
) : Route
