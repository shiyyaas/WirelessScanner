package app.wirelessscanner.wifi.aggregator

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.wifi.model.DiscoveredWifiNetwork

class WifiDeviceAggregator(
    private val maxHistorySize: Int = 30
) {
    private val networksMap = mutableMapOf<String, NetworkRecord>()

    private data class NetworkRecord(
        val bssid: String,
        var ssid: String?,
        var latestObservation: Observation,
        var observationCount: Int,
        val firstSeenEpochMillis: Long,
        var lastSeenEpochMillis: Long,
        val rssiHistory: MutableList<Int>,
        var frequency: Int,
        var capabilities: String,
        var channelWidth: Int
    )

    fun ingest(observation: Observation) {
        val bssid = observation.rawAddress
        val rssi = observation.signalStrength.dbm
        val frequency = observation.metadata["frequency"]?.toIntOrNull() ?: 2412
        val capabilities = observation.metadata["capabilities"] ?: ""
        val channelWidth = observation.metadata["channelWidth"]?.toIntOrNull() ?: 20

        val existing = networksMap[bssid]
        if (existing != null) {
            existing.observationCount++
            existing.lastSeenEpochMillis = observation.timestampEpochMillis
            existing.latestObservation = observation
            if (!observation.advertisedName.isNullOrBlank()) {
                existing.ssid = observation.advertisedName
            }
            existing.frequency = frequency
            existing.capabilities = capabilities
            existing.channelWidth = channelWidth

            existing.rssiHistory.add(rssi)
            if (existing.rssiHistory.size > maxHistorySize) {
                existing.rssiHistory.removeAt(0)
            }
        } else {
            networksMap[bssid] = NetworkRecord(
                bssid = bssid,
                ssid = observation.advertisedName,
                latestObservation = observation,
                observationCount = 1,
                firstSeenEpochMillis = observation.timestampEpochMillis,
                lastSeenEpochMillis = observation.timestampEpochMillis,
                rssiHistory = mutableListOf(rssi),
                frequency = frequency,
                capabilities = capabilities,
                channelWidth = channelWidth
            )
        }
    }

    fun getNetworks(): List<DiscoveredWifiNetwork> {
        return networksMap.values.map { record ->
            val medianRssi = calculateMedian(record.rssiHistory)
            DiscoveredWifiNetwork(
                bssid = record.bssid,
                ssid = record.ssid,
                latestObservation = record.latestObservation,
                observationCount = record.observationCount,
                firstSeenEpochMillis = record.firstSeenEpochMillis,
                lastSeenEpochMillis = record.lastSeenEpochMillis,
                rssiHistory = record.rssiHistory.toList(),
                medianRssi = medianRssi,
                frequency = record.frequency,
                capabilities = record.capabilities,
                channelWidth = record.channelWidth
            )
        }.sortedByDescending { it.lastSeenEpochMillis }
    }

    fun clear() {
        networksMap.clear()
    }

    private fun calculateMedian(values: List<Int>): Int {
        if (values.isEmpty()) return -100
        val sorted = values.sorted()
        val middle = sorted.size / 2
        return if (sorted.size % 2 == 0) {
            (sorted[middle - 1] + sorted[middle]) / 2
        } else {
            sorted[middle]
        }
    }
}
