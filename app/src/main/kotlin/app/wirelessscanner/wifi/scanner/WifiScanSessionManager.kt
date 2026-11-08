package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.collector.CollectorStatus
import app.wirelessscanner.collector.RadioCollector
import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.wifi.aggregator.WifiDeviceAggregator
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Scan freshness status indicator.
 */
sealed interface ScanFreshness {
    data object Unknown : ScanFreshness
    data object Fresh : ScanFreshness
    data object Cached : ScanFreshness
    data class Throttled(val message: String) : ScanFreshness
}

/**
 * High-level Wi-Fi discovery session manager implementing [RadioCollector].
 *
 * Responsibilities:
 * - Controls single and recurring scan requests.
 * - Prevents overlapping/concurrent scan requests via [Mutex].
 * - Tracks scan freshness (fresh vs cached results from [WifiManager.EXTRA_RESULTS_UPDATED]).
 * - Ingests scan results into [WifiDeviceAggregator].
 * - Records the timestamp of the last successful scan.
 * - Communicates scan throttling and error states transparently.
 */
class WifiScanSessionManager(
    private val scanner: WifiHardwareScanner,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : RadioCollector {

    override val radioType: RadioType = RadioType.WIFI

    private val mutex = Mutex()
    private var activeJob: Job? = null
    private val aggregator = WifiDeviceAggregator()

    private val _status = MutableStateFlow<CollectorStatus>(CollectorStatus.Idle)
    override val status: StateFlow<CollectorStatus> = _status.asStateFlow()

    private val _observations = MutableStateFlow<List<Observation>>(emptyList())
    override val observations: StateFlow<List<Observation>> = _observations.asStateFlow()

    private val _discoveredAccessPoints = MutableStateFlow<List<DiscoveredWifiAccessPoint>>(emptyList())
    val discoveredAccessPoints: StateFlow<List<DiscoveredWifiAccessPoint>> = _discoveredAccessPoints.asStateFlow()

    private val _lastScanTimestamp = MutableStateFlow<Long?>(null)
    val lastScanTimestamp: StateFlow<Long?> = _lastScanTimestamp.asStateFlow()

    private val _scanFreshness = MutableStateFlow<ScanFreshness>(ScanFreshness.Unknown)
    val scanFreshness: StateFlow<ScanFreshness> = _scanFreshness.asStateFlow()

    val isScanning: Boolean
        get() = _status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting

    override suspend fun startObserving() {
        triggerScan()
    }

    override suspend fun stopObserving() {
        mutex.withLock {
            activeJob?.cancel()
            activeJob = null
            if (_status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting) {
                _status.value = CollectorStatus.Idle
            }
        }
    }

    /**
     * Executes a single Wi-Fi scan cycle.
     */
    suspend fun triggerScan() {
        mutex.withLock {
            if (isScanning) {
                return
            }

            if (!scanner.isWifiAvailable()) {
                _status.value = CollectorStatus.Unavailable("Wi-Fi hardware is unavailable.")
                return
            }

            if (!scanner.isWifiEnabled()) {
                _status.value = CollectorStatus.Unavailable("Wi-Fi is turned off. Please enable Wi-Fi.")
                return
            }

            _status.value = CollectorStatus.Starting

            activeJob = scope.launch(dispatcher) {
                try {
                    _status.value = CollectorStatus.Observing
                    scanner.triggerScan().collect { event ->
                        when (event) {
                            is WifiScanEvent.ScanResults -> {
                                val isFresh = event.isFresh
                                _scanFreshness.value = if (isFresh) ScanFreshness.Fresh else ScanFreshness.Cached
                                _lastScanTimestamp.value = System.currentTimeMillis()

                                val updatedObs = (_observations.value + event.observations).takeLast(MAX_OBSERVATIONS_CACHE)
                                event.observations.forEach { obs ->
                                    aggregator.ingest(obs, isFresh = isFresh)
                                }
                                _observations.value = updatedObs
                                _discoveredAccessPoints.value = aggregator.currentAccessPoints()
                                _status.value = CollectorStatus.Idle
                            }
                            is WifiScanEvent.ScanFailed -> {
                                _scanFreshness.value = ScanFreshness.Throttled(event.reason)
                                if (event.cachedAccessPoints.isNotEmpty()) {
                                    // Process cached results if available
                                    event.cachedObservations.forEach { obs ->
                                        aggregator.ingest(obs, isFresh = false)
                                    }
                                    _discoveredAccessPoints.value = aggregator.currentAccessPoints()
                                }
                                _status.value = CollectorStatus.Error(event.reason)
                            }
                        }
                    }
                } catch (e: Throwable) {
                    _status.value = CollectorStatus.Error("Wi-Fi scan failed: ${e.message}")
                } finally {
                    if (_status.value is CollectorStatus.Observing || _status.value is CollectorStatus.Starting) {
                        _status.value = CollectorStatus.Idle
                    }
                }
            }
        }
    }

    /**
     * Loads currently cached scan results from the Wi-Fi subsystem without issuing a scan request.
     */
    fun loadCachedResults() {
        val cached = scanner.getCachedScanResults()
        if (cached.isNotEmpty()) {
            _discoveredAccessPoints.value = cached
            _scanFreshness.value = ScanFreshness.Cached
        }
    }

    /**
     * Clears all session observations and discovered access points.
     */
    suspend fun clearResults() {
        mutex.withLock {
            aggregator.clear()
            _observations.value = emptyList()
            _discoveredAccessPoints.value = emptyList()
            _scanFreshness.value = ScanFreshness.Unknown
            _lastScanTimestamp.value = null
        }
    }

    companion object {
        const val MAX_OBSERVATIONS_CACHE = 500
    }
}
