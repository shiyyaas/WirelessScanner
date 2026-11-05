package app.wirelessscanner.ble.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Android framework implementation of [BleHardwareScanner] using [BluetoothLeScanner].
 *
 * Guarantees:
 * - Emits via [callbackFlow] with robust cleanup on closure/cancellation.
 * - Handles scan errors cleanly with [BleScanEvent.ScanFailed].
 * - Configures low latency scan mode for foreground discovery.
 * - Prevents crash if adapter or scanner becomes null unexpectedly.
 */
class AndroidBleScanner(
    private val context: Context
) : BleHardwareScanner {

    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothManager?.adapter
    }

    override fun isAvailable(): Boolean = bluetoothAdapter != null

    override fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun startScan(settings: BleScanSettings): Flow<BleScanEvent> = callbackFlow {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            trySend(BleScanEvent.ScanFailed(
                errorCode = -1,
                description = "Bluetooth adapter is null or disabled"
            ))
            close()
            return@callbackFlow
        }

        val scanner: BluetoothLeScanner? = adapter.bluetoothLeScanner
        if (scanner == null) {
            trySend(BleScanEvent.ScanFailed(
                errorCode = -2,
                description = "BluetoothLeScanner is unavailable on this device"
            ))
            close()
            return@callbackFlow
        }

        val scanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                if (result != null) {
                    try {
                        val observation = BleScanResultMapper.map(result)
                        trySend(BleScanEvent.Discovered(observation))
                    } catch (e: Throwable) {
                        // Avoid crashing scan loop on malformed individual advertisement
                    }
                }
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>?) {
                results?.forEach { result ->
                    try {
                        val observation = BleScanResultMapper.map(result)
                        trySend(BleScanEvent.Discovered(observation))
                    } catch (_: Throwable) {
                    }
                }
            }

            override fun onScanFailed(errorCode: Int) {
                val description = when (errorCode) {
                    SCAN_FAILED_ALREADY_STARTED -> "Scan already started (SCAN_FAILED_ALREADY_STARTED)"
                    SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "App registration failed (SCAN_FAILED_APPLICATION_REGISTRATION_FAILED)"
                    SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scanning unsupported (SCAN_FAILED_FEATURE_UNSUPPORTED)"
                    SCAN_FAILED_INTERNAL_ERROR -> "Internal hardware error (SCAN_FAILED_INTERNAL_ERROR)"
                    SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "Out of hardware resources (SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES)"
                    SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Scanning too frequently, throttled by system (SCAN_FAILED_SCANNING_TOO_FREQUENTLY)"
                    else -> "Scan failed with error code $errorCode"
                }
                trySend(BleScanEvent.ScanFailed(errorCode, description))
            }
        }

        val androidScanSettings = ScanSettings.Builder()
            .setScanMode(settings.scanMode)
            .setReportDelay(settings.reportDelayMillis)
            .build()

        try {
            scanner.startScan(null, androidScanSettings, scanCallback)
        } catch (e: Throwable) {
            trySend(BleScanEvent.ScanFailed(
                errorCode = -3,
                description = "Exception starting BLE scan: ${e.message}"
            ))
            close()
            return@callbackFlow
        }

        awaitClose {
            try {
                if (adapter.isEnabled) {
                    scanner.stopScan(scanCallback)
                }
            } catch (_: Throwable) {
                // Adapter may have been disabled or destroyed concurrently
            }
        }
    }

    companion object {
        const val SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5
        const val SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6
    }
}
