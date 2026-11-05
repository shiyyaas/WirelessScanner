package app.wirelessscanner.ble.aggregator

import app.wirelessscanner.ble.model.BleAddressType
import app.wirelessscanner.ble.model.DiscoveredBleDevice
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import java.util.concurrent.ConcurrentHashMap

/**
 * Aggregates incoming [Observation] streams by their raw hardware address during a scan session.
 *
 * Guarantees:
 * - Thread-safe updates across coroutines.
 * - Caps RSSI history to [maxRssiHistory] (default 30 samples).
 * - Computes the rolling median RSSI across the retained window.
 * - Accurately updates observation count and timestamps (first seen and last seen).
 * - Updates advertised name and metadata when subsequent packets supply richer information.
 * - Does not merge separate addresses even if their advertised names are identical.
 */
class BleDeviceAggregator(
    private val maxRssiHistory: Int = 30
) {
    private val devices = ConcurrentHashMap<String, DiscoveredBleDevice>()

    /**
     * Ingest an observation and return the updated [DiscoveredBleDevice].
     */
    fun ingest(observation: Observation): DiscoveredBleDevice {
        require(observation.radioType == RadioType.BLE) {
            "BleDeviceAggregator only processes BLE observations, got ${observation.radioType}"
        }

        val address = observation.rawAddress
        val rssiDbm = observation.signalStrength.dbm
        val timestamp = observation.timestampEpochMillis

        return devices.compute(address) { _, existing ->
            if (existing == null) {
                DiscoveredBleDevice(
                    rawAddress = address,
                    addressType = parseAddressType(observation.metadata["addressType"]),
                    advertisedName = observation.advertisedName,
                    cachedSystemName = observation.metadata["cachedSystemName"],
                    latestRssi = observation.signalStrength,
                    medianRssi = rssiDbm,
                    rssiHistory = listOf(rssiDbm),
                    firstSeenEpochMillis = timestamp,
                    lastSeenEpochMillis = timestamp,
                    observationCount = 1,
                    txPowerLevel = observation.metadata["txPowerLevel"]?.toIntOrNull(),
                    isConnectable = observation.metadata["isConnectable"]?.toBooleanStrictOrNull(),
                    serviceUuids = parseServiceUuids(observation.metadata["serviceUuids"]),
                    manufacturerData = parseManufacturerData(observation.metadata),
                    serviceData = parseServiceData(observation.metadata),
                    rawPayload = observation.rawPayload,
                    metadata = observation.metadata
                )
            } else {
                val updatedHistory = (existing.rssiHistory + rssiDbm).takeLast(maxRssiHistory)
                val updatedMedian = calculateMedian(updatedHistory)
                val updatedAdvertisedName = observation.advertisedName?.takeIf { it.isNotBlank() }
                    ?: existing.advertisedName
                val updatedCachedName = observation.metadata["cachedSystemName"]?.takeIf { it.isNotBlank() }
                    ?: existing.cachedSystemName

                // Merge manufacturer data
                val newMfg = parseManufacturerData(observation.metadata)
                val mergedMfg = if (newMfg.isNotEmpty()) {
                    existing.manufacturerData + newMfg
                } else {
                    existing.manufacturerData
                }

                // Merge service data
                val newSvc = parseServiceData(observation.metadata)
                val mergedSvc = if (newSvc.isNotEmpty()) {
                    existing.serviceData + newSvc
                } else {
                    existing.serviceData
                }

                // Merge service UUIDs
                val newUuids = parseServiceUuids(observation.metadata["serviceUuids"])
                val mergedUuids = if (newUuids.isNotEmpty()) {
                    (existing.serviceUuids + newUuids).distinct()
                } else {
                    existing.serviceUuids
                }

                val mergedPayload = if (observation.rawPayload.isNotEmpty()) {
                    observation.rawPayload
                } else {
                    existing.rawPayload
                }

                existing.copy(
                    advertisedName = updatedAdvertisedName,
                    cachedSystemName = updatedCachedName,
                    latestRssi = observation.signalStrength,
                    medianRssi = updatedMedian,
                    rssiHistory = updatedHistory,
                    lastSeenEpochMillis = maxOf(existing.lastSeenEpochMillis, timestamp),
                    observationCount = existing.observationCount + 1,
                    txPowerLevel = observation.metadata["txPowerLevel"]?.toIntOrNull() ?: existing.txPowerLevel,
                    isConnectable = observation.metadata["isConnectable"]?.toBooleanStrictOrNull() ?: existing.isConnectable,
                    serviceUuids = mergedUuids,
                    manufacturerData = mergedMfg,
                    serviceData = mergedSvc,
                    rawPayload = mergedPayload,
                    metadata = existing.metadata + observation.metadata
                )
            }
        }!!
    }

    /**
     * Returns an immutable snapshot list of all currently aggregated devices.
     */
    fun currentDevices(): List<DiscoveredBleDevice> = devices.values.toList()

    /**
     * Clear all aggregated devices.
     */
    fun clear() {
        devices.clear()
    }

    companion object {
        internal fun calculateMedian(values: List<Int>): Int {
            if (values.isEmpty()) return 0
            val sorted = values.sorted()
            val size = sorted.size
            return if (size % 2 == 1) {
                sorted[size / 2]
            } else {
                (sorted[size / 2 - 1] + sorted[size / 2]) / 2
            }
        }

        private fun parseAddressType(value: String?): BleAddressType = when (value?.uppercase()) {
            "PUBLIC" -> BleAddressType.PUBLIC
            "RANDOM" -> BleAddressType.RANDOM
            else -> BleAddressType.UNKNOWN
        }

        private fun parseServiceUuids(value: String?): List<String> {
            if (value.isNullOrBlank()) return emptyList()
            return value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        }

        private fun parseManufacturerData(metadata: Map<String, String>): Map<Int, List<Byte>> {
            val result = mutableMapOf<Int, List<Byte>>()
            for ((key, value) in metadata) {
                if (key.startsWith("mfg_")) {
                    val idStr = key.removePrefix("mfg_")
                    val id = idStr.toIntOrNull() ?: continue
                    result[id] = hexStringToBytes(value)
                }
            }
            return result
        }

        private fun parseServiceData(metadata: Map<String, String>): Map<String, List<Byte>> {
            val result = mutableMapOf<String, List<Byte>>()
            for ((key, value) in metadata) {
                if (key.startsWith("svc_data_")) {
                    val uuid = key.removePrefix("svc_data_")
                    result[uuid] = hexStringToBytes(value)
                }
            }
            return result
        }

        private fun hexStringToBytes(hex: String): List<Byte> {
            if (hex.isBlank()) return emptyList()
            val cleanHex = hex.replace(" ", "")
            val bytes = mutableListOf<Byte>()
            var i = 0
            while (i < cleanHex.length - 1) {
                val b = cleanHex.substring(i, i + 2).toIntOrNull(16)?.toByte()
                if (b != null) bytes.add(b)
                i += 2
            }
            return bytes
        }
    }
}
