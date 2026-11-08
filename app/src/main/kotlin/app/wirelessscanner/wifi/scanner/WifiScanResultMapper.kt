package app.wirelessscanner.wifi.scanner

import android.net.wifi.ScanResult
import android.os.Build
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import app.wirelessscanner.wifi.util.WifiFrequencyConverter
import java.util.UUID

/**
 * Maps Android [ScanResult] instances into domain [Observation] and [DiscoveredWifiAccessPoint].
 */
object WifiScanResultMapper {

    /**
     * Maps a [ScanResult] to a pure domain [Observation] preserving radio provenance.
     */
    fun toObservation(
        scanResult: ScanResult,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): Observation {
        val bssid = normalizeBssid(scanResult.BSSID)
        val ssid = normalizeSsid(scanResult.SSID)
        val rssiDbm = scanResult.level
        val freq = scanResult.frequency
        val channel = WifiFrequencyConverter.frequencyToChannel(freq)
        val band = WifiFrequencyConverter.frequencyToBand(freq)
        val capabilities = scanResult.capabilities ?: ""
        val securityType = WifiFrequencyConverter.parseSecurityType(capabilities)
        val channelWidth = extractChannelWidth(scanResult)

        val metadata = mutableMapOf<String, String>()
        metadata["bssid"] = bssid
        if (!ssid.isNullOrBlank()) {
            metadata["ssid"] = ssid
        }
        metadata["frequency"] = freq.toString()
        metadata["channel"] = channel.toString()
        metadata["band"] = band.name
        metadata["channelWidth"] = channelWidth
        metadata["securityType"] = securityType.name
        metadata["capabilities"] = capabilities
        metadata["timestamp"] = scanResult.timestamp.toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            metadata["wifiStandard"] = scanResult.wifiStandard.toString()
        }

        val clampedRssi = rssiDbm.coerceIn(SignalStrength.VALID_RANGE)
        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = timestampEpochMillis,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = bssid,
            advertisedName = ssid,
            signalStrength = SignalStrength(clampedRssi),
            rawPayload = emptyList(), // Android Wi-Fi scan results do not expose raw byte frame payload
            metadata = metadata
        )
    }

    /**
     * Maps a [ScanResult] directly to a [DiscoveredWifiAccessPoint].
     */
    fun toAccessPoint(
        scanResult: ScanResult,
        isFresh: Boolean = true,
        timestampEpochMillis: Long = System.currentTimeMillis()
    ): DiscoveredWifiAccessPoint {
        val bssid = normalizeBssid(scanResult.BSSID)
        val ssid = normalizeSsid(scanResult.SSID)
        val rssiDbm = scanResult.level
        val freq = scanResult.frequency
        val channel = WifiFrequencyConverter.frequencyToChannel(freq)
        val band = WifiFrequencyConverter.frequencyToBand(freq)
        val capabilities = scanResult.capabilities ?: ""
        val securityType = WifiFrequencyConverter.parseSecurityType(capabilities)
        val channelWidth = extractChannelWidth(scanResult)

        val metadata = mutableMapOf<String, String>()
        metadata["bssid"] = bssid
        if (!ssid.isNullOrBlank()) metadata["ssid"] = ssid
        metadata["frequency"] = freq.toString()
        metadata["channel"] = channel.toString()
        metadata["band"] = band.name
        metadata["channelWidth"] = channelWidth
        metadata["securityType"] = securityType.name
        metadata["capabilities"] = capabilities
        metadata["timestamp"] = scanResult.timestamp.toString()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            metadata["wifiStandard"] = scanResult.wifiStandard.toString()
        }

        val clampedRssi = rssiDbm.coerceIn(SignalStrength.VALID_RANGE)
        return DiscoveredWifiAccessPoint(
            bssid = bssid,
            ssid = ssid,
            latestRssi = SignalStrength(clampedRssi),
            medianRssi = clampedRssi,
            rssiHistory = listOf(clampedRssi),
            frequencyMhz = freq,
            channel = channel,
            band = band,
            channelWidth = channelWidth,
            securityType = securityType,
            capabilities = capabilities,
            firstSeenEpochMillis = timestampEpochMillis,
            lastSeenEpochMillis = timestampEpochMillis,
            observationCount = 1,
            isFresh = isFresh,
            metadata = metadata
        )
    }

    private fun normalizeBssid(bssid: String?): String {
        return if (bssid.isNullOrBlank()) {
            "00:00:00:00:00:00"
        } else {
            bssid.trim().uppercase()
        }
    }

    private fun normalizeSsid(ssid: String?): String? {
        if (ssid == null) return null
        val clean = ssid.trim().trim('"', '\'')
        return if (clean.isBlank() || clean == "<unknown ssid>") null else clean
    }

    private fun extractChannelWidth(scanResult: ScanResult): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            WifiFrequencyConverter.channelWidthToString(scanResult.channelWidth)
        } else {
            "20 MHz"
        }
    }
}
