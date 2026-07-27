package com.awan.app.core.domain.home.model

import java.time.LocalDate

data class DaySchedule(
    val date: LocalDate,
    val zones: List<DayZone>,
    val sessions: List<DaySession>,
)
