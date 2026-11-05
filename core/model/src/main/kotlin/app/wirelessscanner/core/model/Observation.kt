package app.wirelessscanner.core.model

/**
 * An immutable observation record capturing a passive wireless transmission.
 *
 * Provenance guarantees:
 * 1. [receiverSource] tracks exactly which radio adapter (internal or external USB) observed the signal.
 * 2. [timestampEpochMillis] preserves the observation moment.
 * 3. [rawPayload] uses an immutable [List] of bytes (preventing mutable array exposure).
 * 4. Identical addresses across different radio types or times are NOT presumed to represent the same physical device.
 *
 * @property id Unique identifier for this discrete observation instance.
 * @property timestampEpochMillis Epoch millisecond timestamp of receipt.
 * @property radioType Protocol or radio family (BLE, Wi-Fi, Cellular, etc.).
 * @property receiverSource Source receiver hardware provenance.
 * @property rawAddress Hardware address, BSSID, or broadcast identifier reported by the radio stack.
 * @property advertisedName Advertised name if present in broadcast, or null.
 * @property signalStrength Received signal strength indicator.
 * @property rawPayload Immutable list of raw broadcast or advertisement payload bytes.
 * @property metadata Additional protocol-specific key-value metadata.
 */
data class Observation(
    val id: String,
    val timestampEpochMillis: Long,
    val radioType: RadioType,
    val receiverSource: ReceiverSource,
    val rawAddress: String,
    val advertisedName: String? = null,
    val signalStrength: SignalStrength,
    val rawPayload: List<Byte> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "Observation id must not be blank" }
        require(rawAddress.isNotBlank()) { "Raw address must not be blank" }
        require(timestampEpochMillis > 0) { "Timestamp must be positive" }
    }

    companion object {
        /**
         * Convenience factory that defensively copies a raw ByteArray into an immutable List<Byte>.
         */
        fun createWithByteArray(
            id: String,
            timestampEpochMillis: Long,
            radioType: RadioType,
            receiverSource: ReceiverSource,
            rawAddress: String,
            advertisedName: String? = null,
            signalStrength: SignalStrength,
            rawBytes: ByteArray? = null,
            metadata: Map<String, String> = emptyMap()
        ): Observation {
            return Observation(
                id = id,
                timestampEpochMillis = timestampEpochMillis,
                radioType = radioType,
                receiverSource = receiverSource,
                rawAddress = rawAddress,
                advertisedName = advertisedName,
                signalStrength = signalStrength,
                rawPayload = rawBytes?.toList() ?: emptyList(),
                metadata = metadata.toMap()
            )
        }
    }
}
