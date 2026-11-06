package app.wirelessscanner.wifi.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat

interface WifiPermissionChecker {
    val requiredPermissions: List<String>
    val rationaleDescription: String
    fun hasScanPermission(): Boolean
    fun isWifiEnabled(): Boolean
    fun isLocationServiceEnabled(): Boolean
}

/**
 * Manages runtime permissions, Wi-Fi hardware state, and location service
 * requirements for Wi-Fi discovery across Android API 26-36.
 *
 * Rules:
 * - ACCESS_WIFI_STATE and CHANGE_WIFI_STATE are normal install permissions.
 * - ACCESS_FINE_LOCATION is required at runtime for startScan() / getScanResults()
 *   on Android 8.0-16 (API 26-36) to access SSID and BSSID information.
 * - On Android 13+ (API 33+), NEARBY_WIFI_DEVICES is also declared, but does not
 *   eliminate the requirement for ACCESS_FINE_LOCATION when using WifiManager.startScan().
 * - Location services (GPS / network provider) must be enabled on Android 9+ for the OS
 *   to deliver scan results to foreground applications.
 */
class WifiPermissionManager(
    private val context: Context
) : WifiPermissionChecker {

    private val wifiManager: WifiManager? by lazy {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    }

    private val locationManager: LocationManager? by lazy {
        context.applicationContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    }

    override val requiredPermissions: List<String>
        get() {
            val list = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                list.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            return list
        }

    override fun hasScanPermission(): Boolean {
        return requiredPermissions.all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun isWifiEnabled(): Boolean {
        return wifiManager?.isWifiEnabled == true
    }

    override fun isLocationServiceEnabled(): Boolean {
        val lm = locationManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lm.isLocationEnabled
        } else {
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }
    }

    override val rationaleDescription: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            "Location permission (ACCESS_FINE_LOCATION) and Nearby Wi-Fi Devices permission are required by Android to discover Wi-Fi access points and retrieve SSIDs and BSSIDs."
        } else {
            "Location permission (ACCESS_FINE_LOCATION) is required by Android to discover Wi-Fi access points and retrieve network scan results."
        }
}
