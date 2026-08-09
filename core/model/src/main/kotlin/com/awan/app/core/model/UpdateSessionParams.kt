package com.awan.app.core.model

import java.time.LocalDateTime

data class UpdateSessionParams(
    val start: LocalDateTime? = null,
    val end: LocalDateTime? = null,
    val locked: Boolean? = null
)
