package app.wirelessscanner.ble.aggregator

import app.wirelessscanner.ble.model.BleAddressType
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BleDeviceAggregatorTest {

    private lateinit var aggregator: BleDeviceAggregator

    @Before
    fun setUp() {
        aggregator = BleDeviceAggregator(maxRssiHistory = 5)
    }

    private fun createObservation(
        address: String,
        rssiDbm: Int,
        timestamp: Long,
        advertisedName: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): Observation {
        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = timestamp,
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = address,
            advertisedName = advertisedName,
            signalStrength = SignalStrength(rssiDbm),
            metadata = metadata
        )
    }

    @Test
    fun ingest_createsNewDiscoveredBleDevice() {
        val obs = createObservation(
            address = "AA:BB:CC:DD:EE:01",
            rssiDbm = -60,
            timestamp = 1000L,
            advertisedName = "SmartSensor",
            metadata = mapOf("addressType" to "RANDOM")
        )

        val device = aggregator.ingest(obs)

        assertThat(device.rawAddress).isEqualTo("AA:BB:CC:DD:EE:01")
        assertThat(device.advertisedName).isEqualTo("SmartSensor")
        assertThat(device.displayName).isEqualTo("SmartSensor")
        assertThat(device.firstSeenEpochMillis).isEqualTo(1000L)
        assertThat(device.lastSeenEpochMillis).isEqualTo(1000L)
        assertThat(device.observationCount).isEqualTo(1)
        assertThat(device.latestRssi.dbm).isEqualTo(-60)
        assertThat(device.medianRssi).isEqualTo(-60)
        assertThat(device.rssiHistory).containsExactly(-60)
        assertThat(device.addressType).isEqualTo(BleAddressType.RANDOM)
    }

    @Test
    fun ingest_updatesExistingDeviceAndIncrementsCount() {
        val obs1 = createObservation("AA:BB:CC:DD:EE:02", -70, 1000L)
        val obs2 = createObservation("AA:BB:CC:DD:EE:02", -60, 2000L)
        val obs3 = createObservation("AA:BB:CC:DD:EE:02", -65, 3000L)

        aggregator.ingest(obs1)
        aggregator.ingest(obs2)
        val device = aggregator.ingest(obs3)

        assertThat(device.observationCount).isEqualTo(3)
        assertThat(device.firstSeenEpochMillis).isEqualTo(1000L)
        assertThat(device.lastSeenEpochMillis).isEqualTo(3000L)
        assertThat(device.latestRssi.dbm).isEqualTo(-65)
        assertThat(device.rssiHistory).containsExactly(-70, -60, -65).inOrder()
        assertThat(device.medianRssi).isEqualTo(-65)
    }

    @Test
    fun ingest_capsRssiHistoryToMaxLimit() {
        for (i in 1..8) {
            val obs = createObservation("AA:BB:CC:DD:EE:03", -50 - i, 1000L + i * 100)
            aggregator.ingest(obs)
        }

        val device = aggregator.currentDevices().first { it.rawAddress == "AA:BB:CC:DD:EE:03" }
        // maxRssiHistory is 5
        assertThat(device.rssiHistory).hasSize(5)
        // Values were -51, -52, -53, -54, -55, -56, -57, -58 -> last 5 are -54, -55, -56, -57, -58
        assertThat(device.rssiHistory).containsExactly(-54, -55, -56, -57, -58).inOrder()
        assertThat(device.observationCount).isEqualTo(8)
    }

    @Test
    fun ingest_updatesAdvertisedNameWhenAvailableLater() {
        val obs1 = createObservation("AA:BB:CC:DD:EE:04", -75, 1000L, advertisedName = null)
        val obs2 = createObservation("AA:BB:CC:DD:EE:04", -72, 2000L, advertisedName = "LateBeacon")

        aggregator.ingest(obs1)
        val device = aggregator.ingest(obs2)

        assertThat(device.advertisedName).isEqualTo("LateBeacon")
        assertThat(device.displayName).isEqualTo("LateBeacon")
    }

    @Test
    fun ingest_doesNotMergeSeparateAddressesWithSameName() {
        val obs1 = createObservation("11:22:33:44:55:01", -60, 1000L, advertisedName = "CommonName")
        val obs2 = createObservation("11:22:33:44:55:02", -62, 1100L, advertisedName = "CommonName")

        aggregator.ingest(obs1)
        aggregator.ingest(obs2)

        val devices = aggregator.currentDevices()
        assertThat(devices).hasSize(2)
        val addresses = devices.map { it.rawAddress }
        assertThat(addresses).containsExactly("11:22:33:44:55:01", "11:22:33:44:55:02")
    }

    @Test
    fun calculateMedian_computesCorrectValues() {
        assertThat(BleDeviceAggregator.calculateMedian(listOf(-70))).isEqualTo(-70)
        assertThat(BleDeviceAggregator.calculateMedian(listOf(-80, -60))).isEqualTo(-70)
        assertThat(BleDeviceAggregator.calculateMedian(listOf(-90, -80, -60))).isEqualTo(-80)
        assertThat(BleDeviceAggregator.calculateMedian(listOf(-90, -85, -75, -60))).isEqualTo(-80) // (-85 + -75)/2 = -80
    }
}
