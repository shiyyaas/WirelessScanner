package app.wirelessscanner.wifi.aggregator

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import app.wirelessscanner.wifi.model.WifiBand

class WifiDeviceAggregatorTest {

    private fun createWifiObservation(
        bssid: String,
        ssid: String?,
        rssiDbm: Int,
        timestampEpochMillis: Long = System.currentTimeMillis(),
        frequency: Int = 2412
    ): Observation = Observation(
        id = java.util.UUID.randomUUID().toString(),
        timestampEpochMillis = timestampEpochMillis,
        radioType = RadioType.WIFI,
        receiverSource = ReceiverSource.Internal(),
        rawAddress = bssid,
        advertisedName = ssid,
        signalStrength = SignalStrength(rssiDbm),
        metadata = mapOf(
            "frequency" to frequency.toString(),
            "channel" to "1",
            "band" to WifiBand.BAND_2_4_GHZ.name,
            "capabilities" to "[WPA2-PSK-CCMP][ESS]"
        )
    )

    @Test
    fun ingest_aggregatesSingleObservation() {
        val aggregator = WifiDeviceAggregator()
        val obs = createWifiObservation("00:11:22:33:44:55", "TestNet", -65, 1000L)

        val ap = aggregator.ingest(obs, isFresh = true)

        assertThat(ap.bssid).isEqualTo("00:11:22:33:44:55")
        assertThat(ap.ssid).isEqualTo("TestNet")
        assertThat(ap.latestRssi.dbm).isEqualTo(-65)
        assertThat(ap.medianRssi).isEqualTo(-65)
        assertThat(ap.observationCount).isEqualTo(1)
        assertThat(ap.firstSeenEpochMillis).isEqualTo(1000L)
        assertThat(ap.lastSeenEpochMillis).isEqualTo(1000L)
        assertThat(ap.isFresh).isTrue()
    }

    @Test
    fun ingest_multipleObservations_computesRollingMedianAndUpdatesCount() {
        val aggregator = WifiDeviceAggregator()
        val bssid = "00:11:22:33:44:55"

        aggregator.ingest(createWifiObservation(bssid, "TestNet", -80, 1000L))
        aggregator.ingest(createWifiObservation(bssid, "TestNet", -60, 2000L))
        val ap = aggregator.ingest(createWifiObservation(bssid, "TestNet", -70, 3000L))

        // History: [-80, -60, -70] -> Sorted: [-80, -70, -60] -> Median: -70
        assertThat(ap.medianRssi).isEqualTo(-70)
        assertThat(ap.latestRssi.dbm).isEqualTo(-70)
        assertThat(ap.observationCount).isEqualTo(3)
        assertThat(ap.firstSeenEpochMillis).isEqualTo(1000L)
        assertThat(ap.lastSeenEpochMillis).isEqualTo(3000L)
    }

    @Test
    fun ingest_cachedResults_doesNotIncrementObservationCount() {
        val aggregator = WifiDeviceAggregator()
        val bssid = "AA:BB:CC:DD:EE:FF"

        aggregator.ingest(createWifiObservation(bssid, "CachedNet", -75, 1000L), isFresh = true)
        val cachedAp = aggregator.ingest(createWifiObservation(bssid, "CachedNet", -75, 2000L), isFresh = false)

        assertThat(cachedAp.observationCount).isEqualTo(1)
        assertThat(cachedAp.isFresh).isFalse()
        assertThat(cachedAp.firstSeenEpochMillis).isEqualTo(1000L)
    }

    @Test
    fun ingest_capsRssiHistoryToMaxLimit() {
        val aggregator = WifiDeviceAggregator(maxRssiHistory = 5)
        val bssid = "11:22:33:44:55:66"

        for (i in 1..10) {
            aggregator.ingest(createWifiObservation(bssid, "HistoryNet", -50 - i, 1000L + i), isFresh = true)
        }

        val ap = aggregator.currentAccessPoints().first()
        assertThat(ap.rssiHistory.size).isEqualTo(5)
        assertThat(ap.rssiHistory).containsExactly(-56, -57, -58, -59, -60).inOrder()
        assertThat(ap.observationCount).isEqualTo(10)
    }

    @Test
    fun calculateMedian_handlesOddAndEvenLists() {
        assertThat(WifiDeviceAggregator.calculateMedian(listOf(-70))).isEqualTo(-70)
        assertThat(WifiDeviceAggregator.calculateMedian(listOf(-80, -60))).isEqualTo(-70)
        assertThat(WifiDeviceAggregator.calculateMedian(listOf(-90, -80, -50))).isEqualTo(-80)
        assertThat(WifiDeviceAggregator.calculateMedian(listOf(-90, -80, -70, -60))).isEqualTo(-75)
    }
}
