package app.wirelessscanner.wifi.scanner

import android.net.wifi.ScanResult
import kotlinx.coroutines.flow.Flow

interface WifiHardwareScanner {
    val scanResults: Flow<List<ScanResult>>
    fun startScan(): Boolean
}
