package app.wirelessscanner.ble.model

import app.wirelessscanner.core.model.SignalStrength

/**
 * Address type reported by the Bluetooth radio stack.
 */
enum class BleAddressType {
    PUBLIC,
    RANDOM,
    UNKNOWN
}

/**
 * Aggregated representation of a discovered BLE device within a scanning session.
 *
 * Designed to preserve raw advertisement evidence while providing session-level
 * statistical tracking (observation counts, first/last seen, median RSSI, RSSI history).
 *
 * In accordance with privacy and wireless observation principles:
 * - Separate addresses must NEVER be assumed to represent the same physical device.
 * - Devices with matching names must NOT be merged if their addresses differ.
 * - Byte payloads are strictly immutable.
 */
data class DiscoveredBleDevice(
    val rawAddress: String,
    val addressType: BleAddressType = BleAddressType.UNKNOWN,
    val advertisedName: String? = null,
    val cachedSystemName: String? = null,
    val latestRssi: SignalStrength,
    val medianRssi: Int,
    val rssiHistory: List<Int> = emptyList(),
    val firstSeenEpochMillis: Long,
    val lastSeenEpochMillis: Long,
    val observationCount: Int = 1,
    val txPowerLevel: Int? = null,
    val isConnectable: Boolean? = null,
    val serviceUuids: List<String> = emptyList(),
    val manufacturerData: Map<Int, List<Byte>> = emptyMap(),
    val serviceData: Map<String, List<Byte>> = emptyMap(),
    val rawPayload: List<Byte> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(rawAddress.isNotBlank()) { "rawAddress must not be blank" }
        require(firstSeenEpochMillis > 0) { "firstSeenEpochMillis must be positive" }
        require(lastSeenEpochMillis >= firstSeenEpochMillis) {
            "lastSeenEpochMillis ($lastSeenEpochMillis) cannot be earlier than firstSeenEpochMillis ($firstSeenEpochMillis)"
        }
        require(observationCount >= 1) { "observationCount must be at least 1" }
    }

    /**
     * Preferred display name: advertised local name first, then cached system name, or null if neither.
     */
    val displayName: String?
        get() = advertisedName?.takeIf { it.isNotBlank() }
            ?: cachedSystemName?.takeIf { it.isNotBlank() }
}
