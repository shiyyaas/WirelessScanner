package app.wirelessscanner.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalCategory
import app.wirelessscanner.core.model.SignalStrength

@Entity(tableName = "observations")
data class ObservationEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val timestampEpochMillis: Long,
    val radioType: RadioType,
    val receiverSourceType: String, // "INTERNAL" or "EXTERNAL_USB"
    val receiverSourceIdentifier: String,
    val receiverSourceDisplayName: String,
    val receiverSourceVendorId: Int?,
    val receiverSourceProductId: Int?,
    val receiverSourceFirmwareVersion: String?,
    val rawAddress: String,
    val advertisedName: String?,
    val rssi: Int,
    val signalCategory: SignalCategory,
    val rawPayloadBytes: ByteArray, // Stored as blob
    val metadataJson: String // Serialized map or JSON string
) {
    fun toDomain(): Observation {
        val receiverSource: ReceiverSource = when (receiverSourceType) {
            "EXTERNAL_USB" -> ReceiverSource.ExternalUsb(
                identifier = receiverSourceIdentifier,
                displayName = receiverSourceDisplayName,
                vendorId = receiverSourceVendorId,
                productId = receiverSourceProductId,
                firmwareVersion = receiverSourceFirmwareVersion
            )
            else -> ReceiverSource.Internal(
                identifier = receiverSourceIdentifier,
                displayName = receiverSourceDisplayName
            )
        }

        // Convert byte array to List<Byte>
        val payloadList = rawPayloadBytes.toList()

        // Parse metadataJson into Map<String, String>
        val metadataMap = parseMetadata(metadataJson)

        return Observation(
            id = id,
            timestampEpochMillis = timestampEpochMillis,
            radioType = radioType,
            receiverSource = receiverSource,
            rawAddress = rawAddress,
            advertisedName = advertisedName,
            signalStrength = SignalStrength(rssi),
            rawPayload = payloadList,
            metadata = metadataMap
        )
    }

    companion object {
        fun fromDomain(observation: Observation, sessionId: String): ObservationEntity {
            val (sourceType, vid, pid, fw) = when (val src = observation.receiverSource) {
                is ReceiverSource.Internal -> listOf("INTERNAL", null, null, null)
                is ReceiverSource.ExternalUsb -> listOf("EXTERNAL_USB", src.vendorId, src.productId, src.firmwareVersion)
            }

            val metadataString = observation.metadata.entries.joinToString(";") { "${it.key}=${it.value}" }

            return ObservationEntity(
                id = observation.id,
                sessionId = sessionId,
                timestampEpochMillis = observation.timestampEpochMillis,
                radioType = observation.radioType,
                receiverSourceType = sourceType as String,
                receiverSourceIdentifier = observation.receiverSource.identifier,
                receiverSourceDisplayName = observation.receiverSource.displayName,
                receiverSourceVendorId = vid as Int?,
                receiverSourceProductId = pid as Int?,
                receiverSourceFirmwareVersion = fw as String?,
                rawAddress = observation.rawAddress,
                advertisedName = observation.advertisedName,
                rssi = observation.signalStrength.rssi,
                signalCategory = observation.signalStrength.category,
                rawPayloadBytes = observation.rawPayload.toByteArray(),
                metadataJson = metadataString
            )
        }

        private fun parseMetadata(json: String): Map<String, String> {
            if (json.isBlank()) return emptyMap()
            return json.split(";").associate { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
            }
        }
    }
}
