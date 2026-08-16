package com.awan.app

/** A notification tap asking for one session to be opened. Delivered once, then done. */
data class SessionDeepLink(
    val sessionId: String,
    val date: String?,
)
