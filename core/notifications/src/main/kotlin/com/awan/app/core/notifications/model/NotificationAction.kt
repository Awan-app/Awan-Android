package com.awan.app.core.notifications.model

/** What a notification button does. Stored by name in the intent, so the names are wire format. */
enum class NotificationAction {
    /** Pushes the session forward by the configured snooze length, keeping its duration. */
    SNOOZE,

    /** Marks the session complete as it stands. */
    COMPLETE,

    /** Ends the session at the moment the button was pressed, then completes it. */
    STOP_HERE,
    ;

    companion object {
        fun fromNameOrNull(raw: String?): NotificationAction? =
            entries.firstOrNull { it.name == raw }
    }
}
