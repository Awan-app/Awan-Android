package com.awan.app.core.domain.zones.model

/**
 * A named window of the day a task can be placed into. [startMinutes]/[endMinutes] are
 * minutes-from-midnight; list order is the zone's order. Colors are ARGB so the domain layer
 * stays free of any Android/Compose dependency.
 */
data class Zone(
    val id: String,
    val name: String,
    val colorArgb: Int,
    val startMinutes: Int,
    val endMinutes: Int,
    val isEnabled: Boolean = true,
) {
    val durationMinutes: Int get() = endMinutes - startMinutes

    companion object {
        const val STUDY = "study"
        const val WORK = "work"
        const val PLAY = "play"
        const val PERSONAL = "personal"

        /**
         * The four fixed default zones in canonical order. Windows here are placeholders;
         * the domain model doesn't lay them out — the onboarding suggestion use case does.
         */
        val defaults: List<Zone> = listOf(
            Zone(id = STUDY, name = "Study", colorArgb = 0xFF7A64FF.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = WORK, name = "Work", colorArgb = 0xFF2EAAFF.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = PLAY, name = "Play", colorArgb = 0xFFFF6F91.toInt(), startMinutes = 0, endMinutes = 0),
            Zone(id = PERSONAL, name = "Personal", colorArgb = 0xFFFF9838.toInt(), startMinutes = 0, endMinutes = 0),
        )
    }
}
