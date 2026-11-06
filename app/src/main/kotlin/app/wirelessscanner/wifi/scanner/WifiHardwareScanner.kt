package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.core.model.Observation
import app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint
import kotlinx.coroutines.flow.Flow

/**
 * Scan events emitted during a Wi-Fi discovery attempt.
 */
sealed interface WifiScanEvent {
    /**
     * Results returned by WifiManager.
     * @param accessPoints The parsed access points.
     * @param observations The domain observations.
     * @param isFresh True if EXTRA_RESULTS_UPDATED was true, false if cached/stale results.
     */
    data class ScanResults(
        val accessPoints: List<DiscoveredWifiAccessPoint>,
        val observations: List<Observation>,
        val isFresh: Boolean
    ) : WifiScanEvent

    /**
     * Scan failed or was throttled by Android OS.
     * @param reason The error description.
     * @param cachedAccessPoints Cached results if any were available.
     * @param cachedObservations Cached observations if available.
     */
    data class ScanFailed(
        val reason: String,
        val cachedAccessPoints: List<DiscoveredWifiAccessPoint> = emptyList(),
        val cachedObservations: List<Observation> = emptyList()
    ) : WifiScanEvent
}

/**
 * Hardware abstraction for Wi-Fi scanning.
 */
interface WifiHardwareScanner {
    fun isWifiAvailable(): Boolean
    fun isWifiEnabled(): Boolean
    suspend fun triggerScan(): Flow<WifiScanEvent>
    fun getCachedScanResults(): List<DiscoveredWifiAccessPoint>
}
