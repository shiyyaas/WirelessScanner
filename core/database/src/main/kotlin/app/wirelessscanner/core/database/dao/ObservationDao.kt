package app.wirelessscanner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import app.wirelessscanner.core.database.entity.ObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ObservationDao {
    @Query("SELECT * FROM observations ORDER BY timestampEpochMillis DESC")
    fun getAllObservations(): Flow<List<ObservationEntity>>

    @Query("SELECT * FROM observations WHERE sessionId = :sessionId ORDER BY timestampEpochMillis DESC")
    fun getObservationsForSession(sessionId: String): Flow<List<ObservationEntity>>

    @Query("""
        SELECT * FROM observations
        WHERE rawAddress LIKE '%' || :query || '%'
        OR advertisedName LIKE '%' || :query || '%'
        OR radioType LIKE '%' || :query || '%'
        ORDER BY timestampEpochMillis DESC
    """)
    fun searchObservations(query: String): Flow<List<ObservationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservation(observation: ObservationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObservations(observations: List<ObservationEntity>)

    @Query("DELETE FROM observations WHERE sessionId = :sessionId")
    suspend fun deleteObservationsForSession(sessionId: String)

    @Query("DELETE FROM observations")
    suspend fun deleteAllObservations()
}
