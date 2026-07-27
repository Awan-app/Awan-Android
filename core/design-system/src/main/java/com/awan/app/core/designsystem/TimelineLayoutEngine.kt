package com.awan.app.core.designsystem

import androidx.compose.runtime.Immutable

@Immutable
data class PositionedSession(
    val session: ScheduleSession,
    val startMinutes: Int,
    val durationMinutes: Int,
    val columnIndex: Int,
    val totalColumns: Int,
)

object TimelineLayoutEngine {

    fun calculateLayout(sessions: List<ScheduleSession>): List<PositionedSession> {
        if (sessions.isEmpty()) return emptyList()

        val sorted = sessions.sortedWith(
            compareBy<ScheduleSession> { it.startMinutes }
                .thenByDescending { it.durationMinutes }
        )

        val result = mutableListOf<PositionedSession>()

        val clusters = mutableListOf<MutableList<ScheduleSession>>()
        var currentCluster = mutableListOf<ScheduleSession>()
        var clusterEndMins = -1

        for (session in sorted) {
            val sessionEnd = session.startMinutes + session.durationMinutes
            if (currentCluster.isEmpty()) {
                currentCluster.add(session)
                clusterEndMins = sessionEnd
            } else if (session.startMinutes < clusterEndMins) {
                currentCluster.add(session)
                clusterEndMins = maxOf(clusterEndMins, sessionEnd)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(session)
                clusterEndMins = sessionEnd
            }
        }
        if (currentCluster.isNotEmpty()) {
            clusters.add(currentCluster)
        }

        for (cluster in clusters) {
            val columns = mutableListOf<MutableList<ScheduleSession>>()
            val sessionColumnMap = mutableMapOf<String, Int>()

            for (session in cluster) {
                var assignedCol = -1
                for ((colIdx, colSessions) in columns.withIndex()) {
                    val lastInCol = colSessions.last()
                    val lastEnd = lastInCol.startMinutes + lastInCol.durationMinutes
                    if (session.startMinutes >= lastEnd) {
                        colSessions.add(session)
                        assignedCol = colIdx
                        break
                    }
                }
                if (assignedCol == -1) {
                    columns.add(mutableListOf(session))
                    assignedCol = columns.size - 1
                }
                sessionColumnMap[session.id] = assignedCol
            }

            val totalClusterCols = columns.size

            for (session in cluster) {
                val colIdx = sessionColumnMap[session.id] ?: 0

                result.add(
                    PositionedSession(
                        session = session,
                        startMinutes = session.startMinutes,
                        durationMinutes = session.durationMinutes,
                        columnIndex = colIdx,
                        totalColumns = totalClusterCols,
                    )
                )
            }
        }

        return result
    }
}
