package app.wirelessscanner.ble.scanner

import app.wirelessscanner.ble.aggregator.BleDeviceAggregator
import app.wirelessscanner.ble.model.DiscoveredBleDevice
import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.collector.RadioCollector
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * High-level BLE discovery session manager implementing [RadioCollector].
 *
 * Responsibilities:
 * - Controls timed discovery sessions (default 15 seconds).
 * - Exposes session countdown seconds remaining.
 * - Manages concurrent scan prevention via [Mutex].
 * - Aggregates raw observations into [DiscoveredBleDevice] list.
 * - Handles scanner errors and gracefully stops when session expires or is manually stopped.
 */
class BleScanSessionManager(
    private val scanner: BleHardwareScanner,
    private val scope: CoroutineScope,
    private val defaultDurationSeconds: Int = DEFAULT_SCAN_DURATION_SECONDS,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : RadioCollector {

    override val radioType: RadioType = RadioType.BLE

    private val mutex = Mutex()
    private var scanJob: Job? = null
    private var timerJob: Job? = null

    private val aggregator = BleDeviceAggregator()

    private val _status = MutableStateFlow<CollectorStatus>(CollectorStatus.Idle)
    override val status: StateFlow<CollectorStatus> = _status.asStateFlow()

    private val _observations = MutableStateFlow<List<Observation>>(emptyList())
    override val observations: StateFlow<List<Observation>> = _observations.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<DiscoveredBleDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<DiscoveredBleDevice>> = _discoveredDevices.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    val isScanning: Boolean
        get() = _status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting

    override suspend fun startObserving() {
        startObserving(durationSeconds = defaultDurationSeconds)
    }

    suspend fun startObserving(durationSeconds: Int = defaultDurationSeconds) {
        mutex.withLock {
            if (isScanning) {
                // Prevent duplicate concurrent scanning sessions
                return
            }

            if (!scanner.isAvailable()) {
                _status.value = CollectorStatus.Unavailable("BLE hardware not available on this device")
                return
            }

            if (!scanner.isBluetoothEnabled()) {
                _status.value = CollectorStatus.Unavailable("Bluetooth is currently disabled")
                return
            }

            _status.value = CollectorStatus.Starting
            _secondsRemaining.value = durationSeconds

            val currentScanJob = scope.launch(dispatcher) {
                try {
                    _status.value = CollectorStatus.Observing
                    scanner.startScan(BleScanSettings()).collect { event ->
                        when (event) {
                            is BleScanEvent.Discovered -> {
                                val observation = event.observation
                                val newObservations = (_observations.value + observation).takeLast(MAX_OBSERVATIONS_CACHE)
                                aggregator.ingest(observation)
                                val newDevices = aggregator.currentDevices()
                                _observations.value = newObservations
                                _discoveredDevices.value = newDevices
                            }
                            is BleScanEvent.ScanFailed -> {
                                _status.value = CollectorStatus.Error(event.description)
                                internalStop(keepResults = true)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    if (isActive) {
                        _status.value = CollectorStatus.Error("Scan error: ${e.message}")
                    }
                } finally {
                    if (_status.value is CollectorStatus.Observing) {
                        _status.value = CollectorStatus.Idle
                    }
                }
            }
            scanJob = currentScanJob

            timerJob = scope.launch(dispatcher) {
                var remaining = durationSeconds
                while (remaining > 0 && isActive) {
                    delay(1000L)
                    remaining--
                    _secondsRemaining.value = remaining
                }
                if (isActive) {
                    stopObserving()
                }
            }
        }
    }

    override suspend fun stopObserving() {
        mutex.withLock {
            internalStop(keepResults = true)
        }
    }

    suspend fun clearResults() {
        mutex.withLock {
            aggregator.clear()
            _observations.value = emptyList()
            _discoveredDevices.value = emptyList()
        }
    }

    private fun internalStop(keepResults: Boolean) {
        timerJob?.cancel()
        timerJob = null
        scanJob?.cancel()
        scanJob = null
        _secondsRemaining.value = 0

        if (_status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting) {
            _status.value = CollectorStatus.Idle
        }

        if (!keepResults) {
            aggregator.clear()
            _observations.value = emptyList()
            _discoveredDevices.value = emptyList()
        }
    }

    companion object {
        const val DEFAULT_SCAN_DURATION_SECONDS = 15
        const val MAX_OBSERVATIONS_CACHE = 500
    }
}
