package com.awan.app.core.domain.onboarding

import com.awan.app.core.model.FirstTask
import com.awan.app.core.model.Zone
import java.util.UUID
import javax.inject.Inject

/**
 * The seam for the future scheduling engine. MVP body is a deliberate fake: place the task at the
 * start of the first enabled zone, with the preferred task length as its duration. When the Local
 * Conflict Engine module lands, only this body changes to call it — signature, ViewModel, and
 * repository stay untouched. Takes model types only, so `:core:domain` stays a pure-Kotlin module
 * with no dependency on the data layer.
 *
 * ponytail: fake placement (first slot of the day); replace body with the scheduling-engine call
 * when the engine module lands.
 */
class ScheduleFirstTaskUseCase @Inject constructor() {

    operator fun invoke(title: String, zones: List<Zone>, preferredTaskLengthMinutes: Int): FirstTask {
        val zone = zones.firstOrNull { it.isEnabled } ?: zones.first()
        return FirstTask(
            id = UUID.randomUUID().toString(),
            title = title.trim(),
            zoneId = zone.id,
            startMinutes = zone.startMinutes,
            durationMinutes = preferredTaskLengthMinutes,
        )
    }
}
