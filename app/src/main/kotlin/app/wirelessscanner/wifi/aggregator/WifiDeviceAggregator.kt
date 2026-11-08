package app.wirelessscanner.wifi.aggregator

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import app.wirelessscanner.wifi.util.WifiFrequencyConverter
import java.util.concurrent.ConcurrentHashMap

class WifiDeviceAggregator(
    private val maxRssiHistory: Int = 30
) {
    private val accessPoints = ConcurrentHashMap<String, DiscoveredWifiAccessPoint>()

    fun ingest(observation: Observation, isFresh: Boolean = true): DiscoveredWifiAccessPoint {
        require(observation.radioType == RadioType.WIFI) {
            "WifiDeviceAggregator only processes Wi-Fi observations, got ${observation.radioType}"
        }

        val bssid = observation.rawAddress
        val rssiDbm = observation.signalStrength.dbm
        val timestamp = observation.timestampEpochMillis

        val freqMhz = observation.metadata["frequency"]?.toIntOrNull() ?: 0
        val channel = observation.metadata["channel"]?.toIntOrNull()
            ?: WifiFrequencyConverter.frequencyToChannel(freqMhz)
        val band = observation.metadata["band"]?.let {
            try { WifiBand.valueOf(it) } catch (_: Exception) { null }
        } ?: WifiFrequencyConverter.frequencyToBand(freqMhz)
        val width = observation.metadata["channelWidth"] ?: "Unknown"
        val caps = observation.metadata["capabilities"] ?: ""
        val secType = observation.metadata["securityType"]?.let {
            try { WifiSecurityType.valueOf(it) } catch (_: Exception) { null }
        } ?: WifiFrequencyConverter.parseSecurityType(caps)

        return accessPoints.compute(bssid) { _, existing ->
            if (existing == null) {
                DiscoveredWifiAccessPoint(
                    bssid = bssid,
                    ssid = observation.advertisedName,
                    latestRssi = observation.signalStrength,
                    medianRssi = rssiDbm,
                    rssiHistory = listOf(rssiDbm),
                    frequencyMhz = freqMhz,
                    channel = channel,
                    band = band,
                    channelWidth = width,
                    securityType = secType,
                    capabilities = caps,
                    firstSeenEpochMillis = timestamp,
                    lastSeenEpochMillis = timestamp,
                    observationCount = 1,
                    isFresh = isFresh,
                    metadata = observation.metadata
                )
            } else {
                val isNewObservation = isFresh

                val updatedHistory = if (isNewObservation) {
                    (existing.rssiHistory + rssiDbm).takeLast(maxRssiHistory)
                } else {
                    existing.rssiHistory
                }

                val updatedMedian = if (isNewObservation) {
                    calculateMedian(updatedHistory)
                } else {
                    existing.medianRssi
                }

                val updatedSsid = observation.advertisedName?.takeIf { it.isNotBlank() }
                    ?: existing.ssid

                existing.copy(
                    ssid = updatedSsid,
                    latestRssi = if (isNewObservation) observation.signalStrength else existing.latestRssi,
                    medianRssi = updatedMedian,
                    rssiHistory = updatedHistory,
                    frequencyMhz = if (freqMhz != 0) freqMhz else existing.frequencyMhz,
                    channel = if (channel != 0) channel else existing.channel,
                    band = if (band != WifiBand.UNKNOWN) band else existing.band,
                    channelWidth = if (width != "Unknown") width else existing.channelWidth,
                    securityType = if (secType != WifiSecurityType.UNKNOWN) secType else existing.securityType,
                    capabilities = if (caps.isNotBlank()) caps else existing.capabilities,
                    lastSeenEpochMillis = if (isNewObservation) maxOf(existing.lastSeenEpochMillis, timestamp) else existing.lastSeenEpochMillis,
                    observationCount = if (isNewObservation) existing.observationCount + 1 else existing.observationCount,
                    isFresh = isFresh,
                    metadata = existing.metadata + observation.metadata
                )
            }
        }!!
    }

    fun markAllAsCached() {
        for ((key, ap) in accessPoints) {
            if (ap.isFresh) {
                accessPoints[key] = ap.copy(isFresh = false)
            }
        }
    }

    fun currentAccessPoints(): List<DiscoveredWifiAccessPoint> = accessPoints.values.toList()

    fun clear() {
        accessPoints.clear()
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
    }
}
