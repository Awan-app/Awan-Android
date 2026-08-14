package com.awan.app.core.notifications.model

/** What a notification button does. Stored by name in the intent, so the names are wire format. */
enum class NotificationAction {
    /** Pushes the session forward by the configured snooze length, keeping its duration. */
    SNOOZE,

    /** Marks the session complete as it stands. */
    COMPLETE,

    /** Ends the session at the moment the button was pressed, then completes it. */
    COMPLETE_NOW,

    /**
     * Clears the running session's notification and leaves the session alone — no status change, no
     * time change, no request at all.
     *
     * The live notification is ongoing, so it cannot be swiped away, and [COMPLETE_NOW] is the only
     * other way off the screen. Without this the user's only exit is to claim they finished
     * something they did not.
     */
    DISMISS_LIVE,
    ;

    companion object {
        fun fromNameOrNull(raw: String?): NotificationAction? =
            entries.firstOrNull { it.name == raw }
    }
}
