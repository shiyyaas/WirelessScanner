package app.wirelessscanner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.wirelessscanner.core.database.entity.ScanSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanSessionDao {

    @Query("SELECT * FROM scan_sessions ORDER BY startTimeEpochMillis DESC")
    fun getAllSessions(): Flow<List<ScanSessionEntity>>

    @Query("SELECT * FROM scan_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionById(sessionId: String): ScanSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ScanSessionEntity)

    @Query("UPDATE scan_sessions SET name = :newName WHERE sessionId = :sessionId")
    suspend fun updateSessionName(sessionId: String, newName: String)

    @Query("UPDATE scan_sessions SET endTimeEpochMillis = :endTime WHERE sessionId = :sessionId")
    suspend fun completeSession(sessionId: String, endTime: Long)

    @Query("DELETE FROM scan_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSession(sessionId: String)

    @Query("DELETE FROM scan_sessions")
    suspend fun purgeAllSessions()
}
