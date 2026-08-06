package com.awan.app.core.domain.onboarding.usecase

import com.awan.app.core.domain.zones.model.Zone
import com.awan.app.core.model.Category
import javax.inject.Inject

/**
 * Stamps a `categoryId` on every zone that has none. Each default zone is named after one of the
 * categories the backend seeds at signup, so the match is a plain name lookup. General is the
 * documented fallback; the first category covers a user who renamed or deleted their way out of
 * having one. An empty category list leaves the zones untouched — the repository then skips the
 * template rather than sending a request the backend is certain to reject.
 */
class AssignDefaultCategoriesUseCase @Inject constructor() {

    operator fun invoke(zones: List<Zone>, categories: List<Category>): List<Zone> {
        if (categories.isEmpty()) return zones
        val fallback = categories.firstOrNull { it.name.equals(GENERAL_CATEGORY, ignoreCase = true) }
            ?: categories.first()
        return zones.map { zone ->
            if (zone.categoryId != null) return@map zone
            val match = categories.firstOrNull { it.name.equals(zone.name, ignoreCase = true) } ?: fallback
            zone.copy(categoryId = match.id)
        }
    }

    private companion object {
        const val GENERAL_CATEGORY = "General"
    }
}
