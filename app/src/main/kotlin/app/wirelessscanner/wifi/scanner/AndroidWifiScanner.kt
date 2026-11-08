package app.wirelessscanner.wifi.scanner

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Production Android implementation of [WifiHardwareScanner] using [WifiManager]
 * and [WifiManager.SCAN_RESULTS_AVAILABLE_ACTION] broadcast receiver.
 *
 * Implements platform throttling awareness:
 * - Checks return value of `wifiManager.startScan()`.
 * - If startScan() returns false, it indicates that the scan request was throttled
 *   (or Wi-Fi is disabled/busy). Cached results are returned with an error indicator.
 * - Extracts `EXTRA_RESULTS_UPDATED` boolean from the broadcast intent to distinguish
 *   between fresh scan results and cached results.
 * - Safe registration and unregistration of BroadcastReceiver in callbackFlow awaitClose.
 */
class AndroidWifiScanner(
    private val context: Context
) : WifiHardwareScanner {

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val isScanInProgress = AtomicBoolean(false)

    override fun isWifiAvailable(): Boolean = wifiManager != null

    override fun isWifiEnabled(): Boolean = wifiManager?.isWifiEnabled == true

    @SuppressLint("MissingPermission")
    override fun getCachedScanResults(): List<app.wirelessscanner.wifi.model.DiscoveredWifiAccessPoint> {
        val manager = wifiManager ?: return emptyList()
        return try {
            val rawResults: List<ScanResult> = manager.scanResults ?: emptyList()
            rawResults.map { WifiScanResultMapper.toAccessPoint(it, isFresh = false) }
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    override suspend fun triggerScan(): Flow<WifiScanEvent> = callbackFlow {
        val manager = wifiManager
        if (manager == null) {
            trySend(WifiScanEvent.ScanFailed("Wi-Fi hardware service is not available on this device."))
            close()
            return@callbackFlow
        }

        if (!manager.isWifiEnabled) {
            trySend(WifiScanEvent.ScanFailed("Wi-Fi is currently disabled."))
            close()
            return@callbackFlow
        }

        if (!isScanInProgress.compareAndSet(false, true)) {
            trySend(WifiScanEvent.ScanFailed("A Wi-Fi scan is already in progress. Please wait."))
            close()
            return@callbackFlow
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val resultsUpdated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    try {
                        val scanResults: List<ScanResult> = manager.scanResults ?: emptyList()
                        val accessPoints = scanResults.map {
                            WifiScanResultMapper.toAccessPoint(it, isFresh = resultsUpdated)
                        }
                        val observations = scanResults.map {
                            WifiScanResultMapper.toObservation(it)
                        }

                        if (resultsUpdated) {
                            trySend(
                                WifiScanEvent.ScanResults(
                                    accessPoints = accessPoints,
                                    observations = observations,
                                    isFresh = true
                                )
                            )
                        } else {
                            trySend(
                                WifiScanEvent.ScanResults(
                                    accessPoints = accessPoints,
                                    observations = observations,
                                    isFresh = false
                                )
                            )
                        }
                    } catch (e: SecurityException) {
                        trySend(WifiScanEvent.ScanFailed("Missing location permission to retrieve Wi-Fi scan results: ${e.message}"))
                    } catch (e: Exception) {
                        trySend(WifiScanEvent.ScanFailed("Error retrieving Wi-Fi scan results: ${e.message}"))
                    } finally {
                        isScanInProgress.set(false)
                        close()
                    }
                }
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)

        val success = try {
            @Suppress("DEPRECATION")
            manager.startScan()
        } catch (e: SecurityException) {
            trySend(WifiScanEvent.ScanFailed("Security exception on startScan(): ${e.message}"))
            isScanInProgress.set(false)
            close()
            false
        } catch (e: Exception) {
            trySend(WifiScanEvent.ScanFailed("Exception triggering Wi-Fi scan: ${e.message}"))
            isScanInProgress.set(false)
            close()
            false
        }

        if (!success) {
            // Android 9+ throttles startScan requests (4 times per 2 minutes for foreground apps).
            // When startScan returns false, the OS rejected the scan request, but cached results may still exist.
            try {
                val cachedScanResults: List<ScanResult> = manager.scanResults ?: emptyList()
                val cachedAps = cachedScanResults.map { WifiScanResultMapper.toAccessPoint(it, isFresh = false) }
                val cachedObs = cachedScanResults.map { WifiScanResultMapper.toObservation(it) }
                trySend(
                    WifiScanEvent.ScanFailed(
                        reason = "Scan request was throttled by Android OS (foreground limit reached). Displaying cached results.",
                        cachedAccessPoints = cachedAps,
                        cachedObservations = cachedObs
                    )
                )
            } catch (_: Exception) {
                trySend(
                    WifiScanEvent.ScanFailed(
                        reason = "Scan request was rejected by Android OS (throttled or busy)."
                    )
                )
            }
            isScanInProgress.set(false)
            close()
        }

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {
                // Receiver was already unregistered or not registered
            }
            isScanInProgress.set(false)
        }
    }
}
