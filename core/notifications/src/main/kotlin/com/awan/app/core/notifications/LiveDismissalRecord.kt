package com.awan.app.core.notifications

import com.awan.app.core.notifications.model.SessionWindow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * How a "Stop" press is written down, and when it stops meaning anything.
 *
 * Pure and separate from [LiveNotificationDismissals] so the two rules that can actually break — the
 * round trip through storage, and which records are dead — are testable without a device.
 */
internal object LiveDismissalRecord {

    private const val SEPARATOR = "|"
    private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun encode(window: SessionWindow): String =
        "${window.start.format(ISO)}$SEPARATOR${window.end.format(ISO)}"

    fun decode(raw: String): SessionWindow? {
        val parts = raw.split(SEPARATOR).takeIf { it.size == 2 } ?: return null
        return runCatching {
            SessionWindow(
                start = LocalDateTime.parse(parts[0], ISO),
                end = LocalDateTime.parse(parts[1], ISO),
            )
        }.getOrNull()
    }

    /**
     * A dismissal only means something while the session it names is still running. Past that, the
     * record is dead weight — and without dropping it the store is append-only.
     */
    fun isExpired(window: SessionWindow, now: LocalDateTime): Boolean = !now.isBefore(window.end)
}
