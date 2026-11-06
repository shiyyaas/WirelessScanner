package app.wirelessscanner.wifi.permission

import android.content.Context
import android.content.pm.PackageManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WifiPermissionManagerTest {

    private class MockContext : android.content.ContextWrapper(null) {
        var fineLocationGranted = true
        var nearbyWifiGranted = true

        override fun getApplicationContext(): Context = this

        override fun checkPermission(permission: String, pid: Int, uid: Int): Int {
            return when (permission) {
                android.Manifest.permission.ACCESS_FINE_LOCATION -> {
                    if (fineLocationGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
                }
                "android.permission.NEARBY_WIFI_DEVICES" -> {
                    if (nearbyWifiGranted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
                }
                else -> PackageManager.PERMISSION_GRANTED
            }
        }
    }

    @Test
    fun requiredPermissions_includesFineLocation() {
        val mockContext = MockContext()
        val manager = WifiPermissionManager(mockContext)

        assertThat(manager.requiredPermissions).contains(android.Manifest.permission.ACCESS_FINE_LOCATION)
        assertThat(manager.rationaleDescription).isNotEmpty()
    }
}
