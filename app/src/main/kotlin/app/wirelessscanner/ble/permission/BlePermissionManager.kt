package app.wirelessscanner.ble.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

interface BlePermissionChecker {
    val requiredPermissions: List<String>
    val rationaleDescription: String
    fun hasScanPermission(): Boolean
}

/**
 * Manages runtime Bluetooth and Location permissions required for BLE scanning
 * across supported Android versions (API 26 through 36).
 *
 * Permission Matrix:
 * - Android 8.0 - 11 (API 26-30):
 *   Requires [Manifest.permission.ACCESS_FINE_LOCATION].
 *   BLUETOOTH and BLUETOOTH_ADMIN are install-time permissions.
 *
 * - Android 12+ (API 31+):
 *   Requires [Manifest.permission.BLUETOOTH_SCAN].
 *   BLUETOOTH_CONNECT is intentionally NOT requested unless connection features are needed.
 *   BLUETOOTH_ADVERTISE is intentionally NOT requested as we do not broadcast.
 */
class BlePermissionManager(
    private val context: Context
) : BlePermissionChecker {

    override val requiredPermissions: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            listOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

    override fun hasScanPermission(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    override val rationaleDescription: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            "Nearby devices permission (BLUETOOTH_SCAN) is required on Android 12+ to detect nearby Bluetooth Low Energy advertisements."
        } else {
            "Location permission (ACCESS_FINE_LOCATION) is required by Android 8–11 to receive Bluetooth Low Energy scan results."
        }
}
