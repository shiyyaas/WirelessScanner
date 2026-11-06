package app.wirelessscanner.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Top-level destinations of WirelessScanner.
 */
sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Dashboard",
        icon = Icons.Default.Dashboard
    )

    data object Observations : Screen(
        route = "observations",
        title = "Observations",
        icon = Icons.Default.Bluetooth
    )

    data object Wifi : Screen(
        route = "wifi",
        title = "Wi-Fi",
        icon = Icons.Default.Wifi
    )

    data object Receivers : Screen(
        route = "receivers",
        title = "Receivers",
        icon = Icons.Default.Router
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings
    )

    companion object {
        val topLevelDestinations: List<Screen> by lazy {
            listOf(
                Dashboard,
                Observations,
                Wifi,
                Receivers,
                Settings
            )
        }
    }
}
