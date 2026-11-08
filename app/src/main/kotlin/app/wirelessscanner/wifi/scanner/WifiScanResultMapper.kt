package app.wirelessscanner.wifi.scanner

import android.net.wifi.ScanResult
import android.os.Build
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import java.util.UUID

object WifiScanResultMapper {

    fun map(
        scanResult: ScanResult,
        epochTimestamp: Long = System.currentTimeMillis()
    ): Observation {
        val bssid = scanResult.bssid ?: "00:00:00:00:00:00"
        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            scanResult.wifiSsid?.toString()?.trim('"')?.takeIf { it.isNotBlank() }
        } else {
            null
        } ?: scanResult.SSID?.takeIf { it.isNotBlank() }

        val rssiDbm = scanResult.level.coerceIn(SignalStrength.VALID_RANGE)
        val signalStrength = SignalStrength(rssiDbm)

        val metadata = mutableMapOf<String, String>().apply {
            put("frequency", scanResult.frequency.toString())
            put("capabilities", scanResult.capabilities ?: "")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                put("channelWidth", scanResult.channelWidth.toString())
                put("centerFreq0", scanResult.centerFreq0.toString())
                put("centerFreq1", scanResult.centerFreq1.toString())
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                put("wifiStandard", scanResult.wifiStandard.toString())
            }
        }

        val rawBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            scanResult.informationElements?.flatMap { ie ->
                ie.bytes?.toList() ?: emptyList()
            } ?: emptyList()
        } else {
            emptyList()
        }

        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = epochTimestamp,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = bssid,
            advertisedName = ssid,
            signalStrength = signalStrength,
            rawPayload = rawBytes,
            metadata = metadata
        )
    }
}

data class TestWifiPayload(
    val bssid: String,
    val ssid: String?,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String = "",
    val channelWidth: Int = 20
)

object WifiPayloadMapper {
    fun map(payload: TestWifiPayload, epochTimestamp: Long = System.currentTimeMillis()): Observation {
        val rssiDbm = payload.rssi.coerceIn(SignalStrength.VALID_RANGE)
        val metadata = mapOf(
            "frequency" to payload.frequency.toString(),
            "capabilities" to payload.capabilities,
            "channelWidth" to payload.channelWidth.toString()
        )
        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = epochTimestamp,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = payload.bssid,
            advertisedName = payload.ssid?.takeIf { it.isNotBlank() },
            signalStrength = SignalStrength(rssiDbm),
            rawPayload = emptyList(),
            metadata = metadata
        )
    }
}
