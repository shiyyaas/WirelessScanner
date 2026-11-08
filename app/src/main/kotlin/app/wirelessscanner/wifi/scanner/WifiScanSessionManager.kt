package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.collector.RadioCollector
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.wifi.aggregator.WifiDeviceAggregator
import app.wirelessscanner.wifi.model.DiscoveredWifiNetwork
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

class WifiScanSessionManager(
    private val scanner: WifiHardwareScanner,
    private val scope: CoroutineScope,
    private val defaultDurationSeconds: Int = 15,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : RadioCollector {

    override val radioType: RadioType = RadioType.WIFI

    private var scanJob: Job? = null
    private var timerJob: Job? = null

    private val aggregator = WifiDeviceAggregator()

    private val _status = MutableStateFlow<CollectorStatus>(CollectorStatus.Idle)
    override val status: StateFlow<CollectorStatus> = _status.asStateFlow()

    private val _observations = MutableStateFlow<List<Observation>>(emptyList())
    override val observations: StateFlow<List<Observation>> = _observations.asStateFlow()

    private val _discoveredNetworks = MutableStateFlow<List<DiscoveredWifiNetwork>>(emptyList())
    val discoveredNetworks: StateFlow<List<DiscoveredWifiNetwork>> = _discoveredNetworks.asStateFlow()

    private val _secondsRemaining = MutableStateFlow(0)
    val secondsRemaining: StateFlow<Int> = _secondsRemaining.asStateFlow()

    val isScanning: Boolean
        get() = _status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting

    override suspend fun startObserving() {
        startObserving(durationSeconds = defaultDurationSeconds)
    }

    suspend fun startObserving(durationSeconds: Int = defaultDurationSeconds) {
        if (isScanning) return

        _status.value = CollectorStatus.Starting
        _secondsRemaining.value = durationSeconds

        scanner.startScan()

        scanJob = scope.launch(dispatcher) {
            try {
                _status.value = CollectorStatus.Observing
                scanner.scanResults.collect { results ->
                    val timestamp = System.currentTimeMillis()
                    for (res in results) {
                        val obs = WifiScanResultMapper.map(res, timestamp)
                        aggregator.ingest(obs)
                        val newObservations = (_observations.value + obs).takeLast(500)
                        _observations.value = newObservations
                        _discoveredNetworks.value = aggregator.getNetworks()
                    }
                }
            } catch (e: Throwable) {
                if (isActive) {
                    _status.value = CollectorStatus.Error("Wi-Fi scan error: ${e.message}")
                }
            } finally {
                if (_status.value is CollectorStatus.Observing) {
                    _status.value = CollectorStatus.Idle
                }
            }
        }

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

    override suspend fun stopObserving() {
        timerJob?.cancel()
        timerJob = null
        scanJob?.cancel()
        scanJob = null
        _secondsRemaining.value = 0
        if (_status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting) {
            _status.value = CollectorStatus.Idle
        }
    }

    suspend fun clearResults() {
        aggregator.clear()
        _observations.value = emptyList()
        _discoveredNetworks.value = emptyList()
    }
}
