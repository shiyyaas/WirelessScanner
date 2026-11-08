package app.wirelessscanner.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_sessions")
data class ScanSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val name: String,
    val startTimeEpochMillis: Long,
    val endTimeEpochMillis: Long?,
    val radioType: String,
    val observationCount: Int
)
