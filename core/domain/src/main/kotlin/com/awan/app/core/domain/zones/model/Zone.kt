package com.awan.app.core.domain.zones.model

/**
 * A named window of the day a task can be placed into. [startMinutes]/[endMinutes] are
 * minutes-from-midnight; list order is the zone's order. Colors are ARGB so the domain layer
 * stays free of any Android/Compose dependency.
 *
 * [categoryId] points at a category the signed-in user owns. The backend rejects a zone without one,
 * so it is nullable only to cover the window before the category list has loaded.
 */
data class Zone(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val isEnabled: Boolean = true,
    val categoryId: String? = null,
) {
    val durationMinutes: Int get() = endMinutes - startMinutes

    companion object {
        const val WORK = "work"
        const val LEARNING = "learning"
        const val PERSONAL = "personal"
        const val GENERAL = "general"

        /**
         * The four fixed default zones in canonical order. Windows here are placeholders;
         * the domain model doesn't lay them out — the onboarding suggestion use case does.
         *
         * Every name matches one of the categories the backend seeds at signup, so the default
         * category for a zone is a plain name lookup rather than a mapping table.
         */
        val defaults: List<Zone> = listOf(
            Zone(id = WORK, name = "Work", colorArgb = 0xFF2EAAFF.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = LEARNING, name = "Learning", colorArgb = 0xFF7A64FF.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = PERSONAL, name = "Personal", colorArgb = 0xFFFF9838.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = GENERAL, name = "General", colorArgb = 0xFFFF6F91.toInt(), startMinutes = 0, endMinutes = 0),
        )
    }
}
