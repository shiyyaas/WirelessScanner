package app.wirelessscanner.ui.screens.observations

import app.wirelessscanner.ble.permission.BlePermissionChecker
import app.wirelessscanner.ble.permission.BluetoothHardwareState
import app.wirelessscanner.ble.permission.BluetoothStateObserver
import app.wirelessscanner.ble.scanner.BleHardwareScanner
import app.wirelessscanner.ble.scanner.BleScanEvent
import app.wirelessscanner.ble.scanner.BleScanSessionManager
import app.wirelessscanner.ble.scanner.BleScanSettings
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.UUID

private class FakeHardwareScanner : BleHardwareScanner {
    val events = MutableSharedFlow<BleScanEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override fun isAvailable(): Boolean = true
    override fun isBluetoothEnabled(): Boolean = true
    override fun startScan(settings: BleScanSettings): Flow<BleScanEvent> = events
}

private class FakePermissionChecker(
    var hasPermission: Boolean = true
) : BlePermissionChecker {
    override val requiredPermissions: List<String> = listOf("android.permission.BLUETOOTH_SCAN")
    override val rationaleDescription: String = "Test rationale"
    override fun hasScanPermission(): Boolean = hasPermission
}

private class FakeBluetoothStateObserver(
    var state: BluetoothHardwareState = BluetoothHardwareState.Enabled
) : BluetoothStateObserver {
    override val currentState: BluetoothHardwareState get() = state
    override fun observeState(): Flow<BluetoothHardwareState> = flowOf(state)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ObservationsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createObservation(address: String, name: String?, rssi: Int, timestamp: Long): Observation {
        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = timestamp,
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = address,
            advertisedName = name,
            signalStrength = SignalStrength(rssi)
        )
    }

    @Test
    fun initialState_isIdle() = runTest(testDispatcher) {
        val scanner = FakeHardwareScanner()
        val sessionManager = BleScanSessionManager(scanner, backgroundScope, dispatcher = testDispatcher)
        val permManager = FakePermissionChecker(hasPermission = true)
        val btManager = FakeBluetoothStateObserver(BluetoothHardwareState.Enabled)

        val viewModel = ObservationsViewModel(sessionManager, permManager, btManager)
        testScheduler.runCurrent()

        assertThat(viewModel.uiState.value.scanUiStatus).isEqualTo(ScanUiStatus.Idle)
        assertThat(viewModel.uiState.value.displayedDevices).isEmpty()
    }

    @Test
    fun permissionDeniedState_shownWhenPermissionMissing() = runTest(testDispatcher) {
        val scanner = FakeHardwareScanner()
        val sessionManager = BleScanSessionManager(scanner, backgroundScope, dispatcher = testDispatcher)
        val permManager = FakePermissionChecker(hasPermission = false)
        val btManager = FakeBluetoothStateObserver(BluetoothHardwareState.Enabled)

        val viewModel = ObservationsViewModel(sessionManager, permManager, btManager)
        testScheduler.runCurrent()

        assertThat(viewModel.uiState.value.scanUiStatus).isEqualTo(ScanUiStatus.PermissionDenied)
    }

    @Test
    fun bluetoothDisabledState_shownWhenAdapterDisabled() = runTest(testDispatcher) {
        val scanner = FakeHardwareScanner()
        val sessionManager = BleScanSessionManager(scanner, backgroundScope, dispatcher = testDispatcher)
        val permManager = FakePermissionChecker(hasPermission = true)
        val btManager = FakeBluetoothStateObserver(BluetoothHardwareState.Disabled)

        val viewModel = ObservationsViewModel(sessionManager, permManager, btManager)
        testScheduler.runCurrent()

        assertThat(viewModel.uiState.value.scanUiStatus).isEqualTo(ScanUiStatus.BluetoothDisabled)
    }

    @Test
    fun sortingAndFiltering_operatesCorrectlyOnDiscoveredDevices() = runTest(testDispatcher) {
        val scanner = FakeHardwareScanner()
        val sessionManager = BleScanSessionManager(scanner, backgroundScope, dispatcher = testDispatcher)
        val permManager = FakePermissionChecker(hasPermission = true)
        val btManager = FakeBluetoothStateObserver(BluetoothHardwareState.Enabled)

        val viewModel = ObservationsViewModel(sessionManager, permManager, btManager)
        testScheduler.runCurrent()

        sessionManager.startObserving()
        testScheduler.runCurrent()

        // Ingest two devices with distinct RSSI and lastSeen timestamps
        val obs1 = createObservation("11:11:11:11:11:11", "AlphaBeacon", -50, 1000L)
        val obs2 = createObservation("22:22:22:22:22:22", null, -80, 2000L)

        scanner.events.emit(BleScanEvent.Discovered(obs1))
        testScheduler.runCurrent()

        scanner.events.emit(BleScanEvent.Discovered(obs2))
        testScheduler.runCurrent()

        assertThat(sessionManager.discoveredDevices.value).hasSize(2)
        assertThat(viewModel.uiState.value.displayedDevices).hasSize(2)

        // Sort by STRONGEST_SIGNAL (default): AlphaBeacon (-50) first, unnamed (-80) second
        assertThat(viewModel.uiState.value.displayedDevices.first().rawAddress).isEqualTo("11:11:11:11:11:11")

        // Sort by MOST_RECENT: obs2 (2000L) first, obs1 (1000L) second
        viewModel.setSortOrder(ObservationSortOrder.MOST_RECENT)
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.displayedDevices.first().rawAddress).isEqualTo("22:22:22:22:22:22")

        // Filter NAMED_ONLY: only AlphaBeacon
        viewModel.setFilterMode(NameFilterMode.NAMED_ONLY)
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.displayedDevices).hasSize(1)
        assertThat(viewModel.uiState.value.displayedDevices.first().rawAddress).isEqualTo("11:11:11:11:11:11")

        // Search query filter
        viewModel.setFilterMode(NameFilterMode.ALL)
        viewModel.setSearchQuery("22:22")
        testScheduler.runCurrent()
        assertThat(viewModel.uiState.value.displayedDevices).hasSize(1)
        assertThat(viewModel.uiState.value.displayedDevices.first().rawAddress).isEqualTo("22:22:22:22:22:22")
    }
}
