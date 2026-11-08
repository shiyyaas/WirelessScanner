package app.wirelessscanner.ui.screens.wifi

import app.wirelessscanner.core.model.SignalStrength
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import app.wirelessscanner.wifi.permission.WifiPermissionChecker
import app.wirelessscanner.wifi.scanner.WifiHardwareScanner
import app.wirelessscanner.wifi.scanner.WifiScanEvent
import app.wirelessscanner.wifi.scanner.WifiScanResultMapper
import app.wirelessscanner.wifi.scanner.WifiScanSessionManager
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WifiViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class FakeWifiHardwareScanner : WifiHardwareScanner {
        override fun isWifiAvailable(): Boolean = true
        override fun isWifiEnabled(): Boolean = true
        override fun getCachedScanResults(): List<DiscoveredWifiAccessPoint> = emptyList()
        override suspend fun triggerScan(): Flow<WifiScanEvent> = flow {}
    }

    private class FakeWifiPermissionChecker(
        var scanPerm: Boolean = true,
        var wifiOn: Boolean = true,
        var locationOn: Boolean = true
    ) : WifiPermissionChecker {
        override val requiredPermissions: List<String> = listOf("android.permission.ACCESS_FINE_LOCATION")
        override val rationaleDescription: String = "Test rationale"
        override fun hasScanPermission(): Boolean = scanPerm
        override fun isWifiEnabled(): Boolean = wifiOn
        override fun isLocationServiceEnabled(): Boolean = locationOn
    }

    private fun sampleAccessPoint(
        bssid: String,
        ssid: String?,
        rssiDbm: Int,
        channel: Int = 1,
        frequency: Int = 2412,
        band: WifiBand = WifiBand.BAND_2_4_GHZ,
        securityType: WifiSecurityType = WifiSecurityType.WPA2
    ): DiscoveredWifiAccessPoint = DiscoveredWifiAccessPoint(
        bssid = bssid,
        ssid = ssid,
        latestRssi = SignalStrength(rssiDbm),
        medianRssi = rssiDbm,
        rssiHistory = listOf(rssiDbm),
        frequencyMhz = frequency,
        channel = channel,
        band = band,
        channelWidth = "20 MHz",
        securityType = securityType,
        capabilities = "[WPA2-PSK-CCMP][ESS]",
        firstSeenEpochMillis = 1000L,
        lastSeenEpochMillis = 1000L,
        isFresh = true
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun uiState_permissionDenied_reflectsStatus() = runTest(testDispatcher) {
        val fakePerm = FakeWifiPermissionChecker(scanPerm = false)
        val sessionManager = WifiScanSessionManager(
            scanner = FakeWifiHardwareScanner(),
            scope = testScope,
            dispatcher = testDispatcher
        )
        val viewModel = WifiViewModel(sessionManager, fakePerm)

        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(WifiUiStatus.PermissionDenied)
    }

    @Test
    fun uiState_wifiDisabled_reflectsStatus() = runTest(testDispatcher) {
        val fakePerm = FakeWifiPermissionChecker(scanPerm = true, wifiOn = false)
        val sessionManager = WifiScanSessionManager(
            scanner = FakeWifiHardwareScanner(),
            scope = testScope,
            dispatcher = testDispatcher
        )
        val viewModel = WifiViewModel(sessionManager, fakePerm)

        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(WifiUiStatus.WifiDisabled)
    }

    @Test
    fun uiState_locationServiceDisabled_reflectsStatus() = runTest(testDispatcher) {
        val fakePerm = FakeWifiPermissionChecker(scanPerm = true, wifiOn = true, locationOn = false)
        val sessionManager = WifiScanSessionManager(
            scanner = FakeWifiHardwareScanner(),
            scope = testScope,
            dispatcher = testDispatcher
        )
        val viewModel = WifiViewModel(sessionManager, fakePerm)

        testScheduler.advanceUntilIdle()

        assertThat(viewModel.uiState.value.status).isEqualTo(WifiUiStatus.LocationServiceDisabled)
    }

    @Test
    fun uiState_sortingAndFiltering_appliesProperly() = runTest(testDispatcher) {
        val ap1 = sampleAccessPoint("00:00:00:00:00:01", "AlphaNet", -50, channel = 1, band = WifiBand.BAND_2_4_GHZ)
        val ap2 = sampleAccessPoint("00:00:00:00:00:02", "BetaNet", -70, channel = 36, band = WifiBand.BAND_5_GHZ)
        val ap3 = sampleAccessPoint("00:00:00:00:00:03", "GammaNet", -60, channel = 6, band = WifiBand.BAND_2_4_GHZ)

        val scanner = object : WifiHardwareScanner {
            override fun isWifiAvailable(): Boolean = true
            override fun isWifiEnabled(): Boolean = true
            override fun getCachedScanResults(): List<DiscoveredWifiAccessPoint> = emptyList()
            override suspend fun triggerScan(): Flow<WifiScanEvent> = flow {
                emit(
                    WifiScanEvent.ScanResults(
                        accessPoints = listOf(ap1, ap2, ap3),
                        observations = listOf(
                            WifiScanResultMapper.toObservation(
                                android.net.wifi.ScanResult().apply {
                                    BSSID = ap1.bssid
                                    SSID = ap1.ssid
                                    level = ap1.latestRssi.dbm
                                    frequency = ap1.frequencyMhz
                                    capabilities = ap1.capabilities
                                }
                            ),
                            WifiScanResultMapper.toObservation(
                                android.net.wifi.ScanResult().apply {
                                    BSSID = ap2.bssid
                                    SSID = ap2.ssid
                                    level = ap2.latestRssi.dbm
                                    frequency = 5180
                                    capabilities = ap2.capabilities
                                }
                            ),
                            WifiScanResultMapper.toObservation(
                                android.net.wifi.ScanResult().apply {
                                    BSSID = ap3.bssid
                                    SSID = ap3.ssid
                                    level = ap3.latestRssi.dbm
                                    frequency = ap3.frequencyMhz
                                    capabilities = ap3.capabilities
                                }
                            )
                        ),
                        isFresh = true
                    )
                )
            }
        }

        val sessionManager = WifiScanSessionManager(
            scanner = scanner,
            scope = testScope,
            dispatcher = testDispatcher
        )
        val viewModel = WifiViewModel(sessionManager, FakeWifiPermissionChecker())

        viewModel.startScan()
        testScheduler.advanceUntilIdle()

        // Verify total AP count
        assertThat(viewModel.uiState.value.totalAccessPointsCount).isEqualTo(3)

        // Filter by 2.4 GHz
        viewModel.setSelectedBand(WifiBand.BAND_2_4_GHZ)
        testScheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.displayedAccessPoints).hasSize(2)
        assertThat(viewModel.uiState.value.displayedAccessPoints.map { it.ssid }).containsExactly("AlphaNet", "GammaNet")

        // Sort by SSID
        viewModel.setSelectedBand(null)
        viewModel.setSortOrder(WifiSortOrder.SSID)
        testScheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.displayedAccessPoints.map { it.ssid }).containsExactly("AlphaNet", "BetaNet", "GammaNet").inOrder()

        // Filter by search query
        viewModel.setSearchQuery("Beta")
        testScheduler.advanceUntilIdle()
        assertThat(viewModel.uiState.value.displayedAccessPoints).hasSize(1)
        assertThat(viewModel.uiState.value.displayedAccessPoints[0].ssid).isEqualTo("BetaNet")
    }

    @Test
    fun uiState_channelMetrics_computedAccurately() = runTest(testDispatcher) {
        val ap1 = sampleAccessPoint("00:00:00:00:00:01", "Net1", -50, channel = 1, band = WifiBand.BAND_2_4_GHZ)
        val ap2 = sampleAccessPoint("00:00:00:00:00:02", "Net2", -70, channel = 1, band = WifiBand.BAND_2_4_GHZ)
        val ap3 = sampleAccessPoint("00:00:00:00:00:03", "Net3", -60, channel = 6, band = WifiBand.BAND_2_4_GHZ)

        val scanner = object : WifiHardwareScanner {
            override fun isWifiAvailable(): Boolean = true
            override fun isWifiEnabled(): Boolean = true
            override fun getCachedScanResults(): List<DiscoveredWifiAccessPoint> = emptyList()
            override suspend fun triggerScan(): Flow<WifiScanEvent> = flow {
                emit(
                    WifiScanEvent.ScanResults(
                        accessPoints = listOf(ap1, ap2, ap3),
                        observations = listOf(
                            WifiScanResultMapper.toObservation(android.net.wifi.ScanResult().apply {
                                BSSID = ap1.bssid; SSID = ap1.ssid; level = -50; frequency = 2412; capabilities = "[ESS]"
                            }),
                            WifiScanResultMapper.toObservation(android.net.wifi.ScanResult().apply {
                                BSSID = ap2.bssid; SSID = ap2.ssid; level = -70; frequency = 2412; capabilities = "[ESS]"
                            }),
                            WifiScanResultMapper.toObservation(android.net.wifi.ScanResult().apply {
                                BSSID = ap3.bssid; SSID = ap3.ssid; level = -60; frequency = 2437; capabilities = "[ESS]"
                            })
                        ),
                        isFresh = true
                    )
                )
            }
        }

        val sessionManager = WifiScanSessionManager(
            scanner = scanner,
            scope = testScope,
            dispatcher = testDispatcher
        )
        val viewModel = WifiViewModel(sessionManager, FakeWifiPermissionChecker())

        viewModel.startScan()
        testScheduler.advanceUntilIdle()

        val metrics = viewModel.uiState.value.channelMetrics
        assertThat(metrics).hasSize(2)

        // Channel 1: 2 APs, average (-50 + -70)/2 = -60
        val ch1 = metrics.first { it.channel == 1 }
        assertThat(ch1.count).isEqualTo(2)
        assertThat(ch1.averageRssiDbm).isEqualTo(-60)

        // Channel 6: 1 AP, average -60
        val ch6 = metrics.first { it.channel == 6 }
        assertThat(ch6.count).isEqualTo(1)
        assertThat(ch6.averageRssiDbm).isEqualTo(-60)
    }
}
