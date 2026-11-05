package app.wirelessscanner.ble.scanner

import app.wirelessscanner.core.model.Observation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Scan events emitted during BLE scanning.
 */
sealed interface BleScanEvent {
    data class Discovered(val observation: Observation) : BleScanEvent
    data class ScanFailed(val errorCode: Int, val description: String) : BleScanEvent
}

/**
 * Scan configuration parameters.
 */
data class BleScanSettings(
    val scanMode: Int = android.bluetooth.le.ScanSettings.SCAN_MODE_LOW_LATENCY,
    val reportDelayMillis: Long = 0L
)

/**
 * Abstraction over Android BluetoothLeScanner hardware to allow unit testing with fakes.
 */
interface BleHardwareScanner {
    fun isAvailable(): Boolean
    fun isBluetoothEnabled(): Boolean
    fun startScan(settings: BleScanSettings): Flow<BleScanEvent>
}
