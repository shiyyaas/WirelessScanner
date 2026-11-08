package app.wirelessscanner.wifi.aggregator

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WifiDeviceAggregatorTest {

    @Test
    fun ingest_aggregatesUniqueNetworksAndRssiHistory() {
        val aggregator = WifiDeviceAggregator()
        val obs1 = Observation(
            id = "1",
            timestampEpochMillis = 1000L,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = "11:22:33:44:55:66",
            advertisedName = "HomeNetwork",
            signalStrength = SignalStrength(-60),
            metadata = mapOf("frequency" to "2412", "capabilities" to "[WPA2]")
        )
        val obs2 = Observation(
            id = "2",
            timestampEpochMillis = 2000L,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = "11:22:33:44:55:66",
            advertisedName = "HomeNetwork",
            signalStrength = SignalStrength(-50),
            metadata = mapOf("frequency" to "2412", "capabilities" to "[WPA2]")
        )

        aggregator.ingest(obs1)
        aggregator.ingest(obs2)

        val networks = aggregator.getNetworks()
        assertThat(networks).hasSize(1)
        val net = networks[0]
        assertThat(net.bssid).isEqualTo("11:22:33:44:55:66")
        assertThat(net.ssid).isEqualTo("HomeNetwork")
        assertThat(net.observationCount).isEqualTo(2)
        assertThat(net.rssiHistory).containsExactly(-60, -50).inOrder()
        assertThat(net.medianRssi).isEqualTo(-55)
    }
}
