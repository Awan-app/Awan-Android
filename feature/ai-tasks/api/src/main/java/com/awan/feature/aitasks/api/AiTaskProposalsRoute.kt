package com.awan.feature.aitasks.api

import com.awan.core.navigation.Route
import kotlinx.serialization.Serializable

/**
 * What the user asked Awan to turn into tasks — a typed note, an optional note alongside a photo,
 * or both. [imageUri] is a `content://` string good for this process's lifetime only; the route is
 * not meant to survive process death.
 */
@Serializable
data class AiTaskProposalsRoute(
    val text: String = "",
    val note: String? = null,
    val imageUri: String? = null,
    val goalId: String? = null,
) : Route
