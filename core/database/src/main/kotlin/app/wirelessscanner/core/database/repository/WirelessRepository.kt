package app.wirelessscanner.core.database.repository

import android.content.Context
import app.wirelessscanner.core.database.WirelessScannerDatabase
import app.wirelessscanner.core.database.entity.ObservationEntity
import app.wirelessscanner.core.database.entity.ScanSessionEntity
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * Repository providing high-level operations for storing, querying, exporting,
 * and purging wireless observations and scan sessions using Room persistence.
 */
class WirelessRepository(context: Context) {

    private val database = WirelessScannerDatabase.getDatabase(context)
    private val observationDao = database.observationDao()
    private val scanSessionDao = database.scanSessionDao()

    /**
     * Flow of all recorded observations across sessions, ordered by timestamp descending.
     */
    val allObservations: Flow<List<Observation>> = observationDao.getAllObservationsFlow().map { entities ->
        entities.map { it.toDomainModel() }
    }

    /**
     * Flow of all scan sessions.
     */
    val allScanSessions: Flow<List<ScanSessionEntity>> = scanSessionDao.getAllSessionsFlow()

    /**
     * Insert a new scan session.
     */
    suspend fun insertSession(session: ScanSessionEntity) {
        scanSessionDao.insertSession(session)
    }

    /**
     * Update an existing scan session name or details.
     */
    suspend fun updateSession(session: ScanSessionEntity) {
        scanSessionDao.updateSession(session)
    }

    /**
     * Update the name of a scan session by ID.
     */
    suspend fun updateSessionName(sessionId: String, newName: String) {
        val session = scanSessionDao.getSessionById(sessionId)
        if (session != null) {
            scanSessionDao.updateSession(session.copy(name = newName))
        }
    }

    /**
     * Insert a list of observations into the database.
     */
    suspend fun insertObservations(observations: List<Observation>, sessionId: String? = null) {
        val entities = observations.map { ObservationEntity.fromDomainModel(it, sessionId) }
        observationDao.insertObservations(entities)
    }

    /**
     * Search observations by query string matching address or advertised name.
     */
    fun searchObservations(query: String): Flow<List<Observation>> {
        return observationDao.searchObservations("%$query%").map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Get observations belonging to a specific scan session.
     */
    fun getObservationsForSession(sessionId: String): Flow<List<Observation>> {
        return observationDao.getObservationsForSession(sessionId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Export all observations as a JSON string for user export/sharing.
     */
    suspend fun exportObservationsToJson(): String {
        val entities = observationDao.getAllObservations()
        val jsonArray = JSONArray()
        for (entity in entities) {
            val obj = JSONObject().apply {
                put("id", entity.id)
                put("timestampEpochMillis", entity.timestampEpochMillis)
                put("radioType", entity.radioType)
                put("receiverIdentifier", entity.receiverIdentifier)
                put("receiverDisplayName", entity.receiverDisplayName)
                put("rawAddress", entity.rawAddress)
                put("advertisedName", entity.advertisedName ?: "")
                put("rssi", entity.rssi)
                put("rawPayloadHex", entity.rawPayloadHex)
                put("sessionId", entity.sessionId ?: "")
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString(2)
    }

    /**
     * Export all observations as a CSV string for user export/sharing.
     */
    suspend fun exportObservationsToCsv(): String {
        val entities = observationDao.getAllObservations()
        val sb = StringBuilder()
        sb.append("id,timestampEpochMillis,radioType,receiverSource,rawAddress,advertisedName,rssi,sessionId\n")
        for (entity in entities) {
            sb.append("\"${entity.id}\",")
            sb.append("${entity.timestampEpochMillis},")
            sb.append("\"${entity.radioType}\",")
            sb.append("\"${entity.receiverDisplayName}\",")
            sb.append("\"${entity.rawAddress}\",")
            sb.append("\"${entity.advertisedName?.replace("\"", "\"\"") ?: ""}\",")
            sb.append("${entity.rssi},")
            sb.append("\"${entity.sessionId ?: ""}\"\n")
        }
        return sb.toString()
    }

    /**
     * Purge all observations and scan sessions from the database.
     */
    suspend fun purgeAllData() {
        observationDao.deleteAllObservations()
        scanSessionDao.deleteAllSessions()
    }

    /**
     * Delete a specific scan session and its associated observations.
     */
    suspend fun deleteSession(sessionId: String) {
        observationDao.deleteObservationsForSession(sessionId)
        scanSessionDao.deleteSessionById(sessionId)
    }

    private fun ObservationEntity.toDomainModel(): Observation {
        val receiverSource = if (receiverIdentifier.startsWith("usb:")) {
            ReceiverSource.ExternalUsb(
                identifier = receiverIdentifier,
                displayName = receiverDisplayName
            )
        } else {
            ReceiverSource.Internal(
                identifier = receiverIdentifier,
                displayName = receiverDisplayName
            )
        }

        val rawBytes = if (rawPayloadHex.isNotBlank()) {
            rawPayloadHex.chunked(2).mapNotNull { it.toIntOrNull(16)?.toByte() }
        } else {
            emptyList()
        }

        return Observation(
            id = id,
            timestampEpochMillis = timestampEpochMillis,
            radioType = RadioType.valueOf(radioType),
            receiverSource = receiverSource,
            rawAddress = rawAddress,
            advertisedName = advertisedName,
            signalStrength = SignalStrength(rssi),
            rawPayload = rawBytes
        )
    }
}
