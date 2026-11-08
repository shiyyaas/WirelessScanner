package app.wirelessscanner.wifi.model

import app.wirelessscanner.core.model.Observation

data class DiscoveredWifiNetwork(
    val bssid: String,
    val ssid: String?,
    val latestObservation: Observation,
    val observationCount: Int,
    val firstSeenEpochMillis: Long,
    val lastSeenEpochMillis: Long,
    val rssiHistory: List<Int>,
    val medianRssi: Int,
    val frequency: Int,
    val capabilities: String,
    val channelWidth: Int
)
