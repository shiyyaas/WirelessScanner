package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.util.UUID

data class TestWifiPayload(
    val bssid: String,
    val ssid: String?,
    val rssi: Int,
    val frequency: Int,
    val capabilities: String = "[WPA2-PSK-CCMP]",
    val channelWidth: Int = 20
)

object TestWifiMapper {
    fun map(payload: TestWifiPayload, epochTimestamp: Long = System.currentTimeMillis()): app.wirelessscanner.core.model.Observation {
        val rssiDbm = payload.rssi.coerceIn(SignalStrength.VALID_RANGE)
        return app.wirelessscanner.core.model.Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = epochTimestamp,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = payload.bssid,
            advertisedName = payload.ssid?.takeIf { it.isNotBlank() },
            signalStrength = SignalStrength(rssiDbm),
            rawPayload = emptyList(),
            metadata = mapOf(
                "frequency" to payload.frequency.toString(),
                "capabilities" to payload.capabilities,
                "channelWidth" to payload.channelWidth.toString()
            )
        )
    }
}

class WifiScanResultMapperTest {

    @Test
    fun map_populatesWifiObservationFields() {
        val payload = TestWifiPayload(
            bssid = "12:34:56:78:9A:BC",
            ssid = "TestWiFiNetwork",
            rssi = -55,
            frequency = 2412,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            channelWidth = 20
        )

        val timestamp = 1700000000000L
        val observation = TestWifiMapper.map(payload, timestamp)

        assertThat(observation.rawAddress).isEqualTo("12:34:56:78:9A:BC")
        assertThat(observation.advertisedName).isEqualTo("TestWiFiNetwork")
        assertThat(observation.radioType).isEqualTo(RadioType.WIFI)
        assertThat(observation.receiverSource).isEqualTo(ReceiverSource.Internal())
        assertThat(observation.signalStrength.dbm).isEqualTo(-55)
        assertThat(observation.timestampEpochMillis).isEqualTo(timestamp)
        assertThat(observation.metadata["frequency"]).isEqualTo("2412")
        assertThat(observation.metadata["capabilities"]).isEqualTo("[WPA2-PSK-CCMP][ESS]")
        assertThat(observation.metadata["channelWidth"]).isEqualTo("20")
    }

    @Test
    fun map_handlesHiddenOrEmptySsidGracefully() {
        val payload = TestWifiPayload(
            bssid = "AA:BB:CC:DD:EE:FF",
            ssid = "",
            rssi = -78,
            frequency = 5180
        )

        val observation = TestWifiMapper.map(payload)

        assertThat(observation.rawAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
        assertThat(observation.advertisedName).isNull()
        assertThat(observation.signalStrength.dbm).isEqualTo(-78)
        assertThat(observation.metadata["frequency"]).isEqualTo("5180")
    }
}
