package app.wirelessscanner.ble.scanner

import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.core.model.SignalStrength
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

class FakeBleHardwareScanner(
    var available: Boolean = true,
    var bluetoothEnabled: Boolean = true
) : BleHardwareScanner {

    val scanEvents = MutableSharedFlow<BleScanEvent>(extraBufferCapacity = 64, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    var scanStartedCount = 0

    override fun isAvailable(): Boolean = available
    override fun isBluetoothEnabled(): Boolean = bluetoothEnabled

    override fun startScan(settings: BleScanSettings): Flow<BleScanEvent> {
        scanStartedCount++
        return scanEvents
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class BleScanSessionManagerTest {

    private fun createObservation(address: String, rssi: Int): Observation {
        return Observation(
            id = UUID.randomUUID().toString(),
            timestampEpochMillis = System.currentTimeMillis(),
            radioType = RadioType.BLE,
            receiverSource = ReceiverSource.Internal(),
            rawAddress = address,
            signalStrength = SignalStrength(rssi)
        )
    }

    @Test
    fun startObserving_unavailableWhenBluetoothDisabled() = runTest {
        val fakeScanner = FakeBleHardwareScanner(bluetoothEnabled = false)
        val manager = BleScanSessionManager(
            scanner = fakeScanner,
            scope = this,
            dispatcher = StandardTestDispatcher(testScheduler)
        )

        manager.startObserving()

        assertThat(manager.status.value).isInstanceOf(CollectorStatus.Unavailable::class.java)
        assertThat((manager.status.value as CollectorStatus.Unavailable).reason).contains("disabled")
        assertThat(fakeScanner.scanStartedCount).isEqualTo(0)
    }

    @Test
    fun startObserving_startsScanAndReceivesObservations() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeScanner = FakeBleHardwareScanner()
        val manager = BleScanSessionManager(
            scanner = fakeScanner,
            scope = this,
            defaultDurationSeconds = 15,
            dispatcher = testDispatcher
        )

        manager.startObserving(15)
        testScheduler.runCurrent()

        assertThat(manager.status.value).isEqualTo(CollectorStatus.Observing)
        assertThat(manager.secondsRemaining.value).isEqualTo(15)
        assertThat(fakeScanner.scanStartedCount).isEqualTo(1)

        val obs = createObservation("AA:BB:CC:DD:EE:FF", -65)
        fakeScanner.scanEvents.emit(BleScanEvent.Discovered(obs))
        testScheduler.runCurrent()

        assertThat(manager.observations.value).hasSize(1)
        assertThat(manager.discoveredDevices.value).hasSize(1)
        assertThat(manager.discoveredDevices.value.first().rawAddress).isEqualTo("AA:BB:CC:DD:EE:FF")
    }

    @Test
    fun startObserving_preventsDuplicateConcurrentSessions() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeScanner = FakeBleHardwareScanner()
        val manager = BleScanSessionManager(
            scanner = fakeScanner,
            scope = this,
            dispatcher = testDispatcher
        )

        manager.startObserving(15)
        testScheduler.runCurrent()
        manager.startObserving(15)
        testScheduler.runCurrent()

        assertThat(fakeScanner.scanStartedCount).isEqualTo(1)
    }

    @Test
    fun startObserving_stopsAutomaticallyAfterDurationExpires() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeScanner = FakeBleHardwareScanner()
        val manager = BleScanSessionManager(
            scanner = fakeScanner,
            scope = this,
            defaultDurationSeconds = 3,
            dispatcher = testDispatcher
        )

        manager.startObserving(3)
        testScheduler.runCurrent()
        assertThat(manager.status.value).isEqualTo(CollectorStatus.Observing)

        advanceTimeBy(3500L)
        testScheduler.runCurrent()

        assertThat(manager.status.value).isEqualTo(CollectorStatus.Idle)
        assertThat(manager.secondsRemaining.value).isEqualTo(0)
    }

    @Test
    fun stopObserving_stopsScanSessionImmediately() = runTest {
        val testDispatcher = StandardTestDispatcher(testScheduler)
        val fakeScanner = FakeBleHardwareScanner()
        val manager = BleScanSessionManager(
            scanner = fakeScanner,
            scope = this,
            defaultDurationSeconds = 15,
            dispatcher = testDispatcher
        )

        manager.startObserving(15)
        testScheduler.runCurrent()
        assertThat(manager.status.value).isEqualTo(CollectorStatus.Observing)

        manager.stopObserving()
        testScheduler.runCurrent()

        assertThat(manager.status.value).isEqualTo(CollectorStatus.Idle)
        assertThat(manager.secondsRemaining.value).isEqualTo(0)
    }
}
