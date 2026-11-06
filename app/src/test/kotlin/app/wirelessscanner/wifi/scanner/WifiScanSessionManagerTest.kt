package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiScanSessionManagerTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeWifiHardwareScanner(
        var available: Boolean = true,
        var enabled: Boolean = true,
        var scanEvents: List<WifiScanEvent> = emptyList(),
        var cachedResults: List<DiscoveredWifiAccessPoint> = emptyList()
    ) : WifiHardwareScanner {
        override fun isWifiAvailable(): Boolean = available
        override fun isWifiEnabled(): Boolean = enabled
        override fun getCachedScanResults(): List<DiscoveredWifiAccessPoint> = cachedResults
        override suspend fun triggerScan(): Flow<WifiScanEvent> = flow {
            scanEvents.forEach { emit(it) }
        }
    }

    private fun sampleAccessPoint(bssid: String, ssid: String?): DiscoveredWifiAccessPoint =
        DiscoveredWifiAccessPoint(
            bssid = bssid,
            ssid = ssid,
            latestRssi = SignalStrength(-65),
            medianRssi = -65,
            frequencyMhz = 2412,
            channel = 1,
            band = WifiBand.BAND_2_4_GHZ,
            channelWidth = "20 MHz",
            securityType = WifiSecurityType.WPA2,
            capabilities = "[WPA2-PSK-CCMP][ESS]",
            firstSeenEpochMillis = 1000L,
            lastSeenEpochMillis = 1000L,
            isFresh = true
        )

    private fun sampleObservation(bssid: String, ssid: String?): Observation =
        Observation(
            id = "obs-1",
            timestampEpochMillis = 1000L,
            radioType = RadioType.WIFI,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = bssid,
            advertisedName = ssid,
            signalStrength = SignalStrength(-65),
            metadata = mapOf("frequency" to "2412", "channel" to "1", "band" to "BAND_2_4_GHZ")
        )

    @Test
    fun triggerScan_successfulFreshScan_updatesDiscoveredAccessPoints() = runTest(testDispatcher) {
        val ap = sampleAccessPoint("00:11:22:33:44:55", "HomeNet")
        val obs = sampleObservation("00:11:22:33:44:55", "HomeNet")
        val fakeScanner = FakeWifiHardwareScanner(
            scanEvents = listOf(
                WifiScanEvent.ScanResults(
                    accessPoints = listOf(ap),
                    observations = listOf(obs),
                    isFresh = true
                )
            )
        )

        val manager = WifiScanSessionManager(
            scanner = fakeScanner,
            scope = testScope,
            dispatcher = testDispatcher
        )

        manager.triggerScan()

        assertThat(manager.discoveredAccessPoints.value).hasSize(1)
        assertThat(manager.discoveredAccessPoints.value[0].bssid).isEqualTo("00:11:22:33:44:55")
        assertThat(manager.scanFreshness.value).isEqualTo(ScanFreshness.Fresh)
        assertThat(manager.status.value).isEqualTo(CollectorStatus.Idle)
        assertThat(manager.lastScanTimestamp.value).isNotNull()
    }

    @Test
    fun triggerScan_scanThrottled_updatesStatusAndLoadsCached() = runTest(testDispatcher) {
        val cachedAp = sampleAccessPoint("00:11:22:33:44:55", "CachedHomeNet")
        val cachedObs = sampleObservation("00:11:22:33:44:55", "CachedHomeNet")
        val fakeScanner = FakeWifiHardwareScanner(
            scanEvents = listOf(
                WifiScanEvent.ScanFailed(
                    reason = "Scan request was throttled by Android OS.",
                    cachedAccessPoints = listOf(cachedAp),
                    cachedObservations = listOf(cachedObs)
                )
            )
        )

        val manager = WifiScanSessionManager(
            scanner = fakeScanner,
            scope = testScope,
            dispatcher = testDispatcher
        )

        manager.triggerScan()

        assertThat(manager.discoveredAccessPoints.value).hasSize(1)
        assertThat(manager.scanFreshness.value).isInstanceOf(ScanFreshness.Throttled::class.java)
        assertThat(manager.status.value).isInstanceOf(CollectorStatus.Error::class.java)
    }

    @Test
    fun triggerScan_wifiDisabled_setsUnavailableStatus() = runTest(testDispatcher) {
        val fakeScanner = FakeWifiHardwareScanner(enabled = false)
        val manager = WifiScanSessionManager(
            scanner = fakeScanner,
            scope = testScope,
            dispatcher = testDispatcher
        )

        manager.triggerScan()

        assertThat(manager.status.value).isInstanceOf(CollectorStatus.Unavailable::class.java)
    }
}
