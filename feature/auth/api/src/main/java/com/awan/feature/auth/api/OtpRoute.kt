package com.awan.feature.auth.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
data class OtpRoute(val email: String) : Route
