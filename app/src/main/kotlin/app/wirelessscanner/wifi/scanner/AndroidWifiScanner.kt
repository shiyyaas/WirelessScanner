package app.wirelessscanner.wifi.scanner

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class AndroidWifiScanner(
    private val context: Context,
    private val wifiManager: WifiManager
) : WifiHardwareScanner {

    override val scanResults: Flow<List<ScanResult>> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true)
                    if (success) {
                        try {
                            if (ContextCompat.checkSelfPermission(
                                context ?: return,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) == PackageManager.PERMISSION_GRANTED) {
                                @Suppress("MissingPermission")
                                val results = wifiManager.scanResults
                                trySend(results ?: emptyList())
                            }
                        } catch (_: SecurityException) {
                            trySend(emptyList())
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, filter)

        try {
            if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
                @Suppress("MissingPermission")
                val initialResults = wifiManager.scanResults
                if (!initialResults.isNullOrEmpty()) {
                    trySend(initialResults)
                }
            }
        } catch (_: SecurityException) {}

        awaitClose {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: IllegalArgumentException) {}
        }
    }

    override fun startScan(): Boolean {
        return try {
            wifiManager.startScan()
        } catch (_: SecurityException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
