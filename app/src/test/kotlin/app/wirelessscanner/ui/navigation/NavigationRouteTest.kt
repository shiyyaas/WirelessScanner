package app.wirelessscanner.ui.navigation

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class NavigationRouteTest {

    @Test
    fun `all top level destinations have unique non-empty routes and titles`() {
        val destinations = Screen.topLevelDestinations

        assertThat(destinations).hasSize(5)

        val routes = destinations.map { it.route }
        assertThat(routes).containsNoDuplicates()
        routes.forEach { route ->
            assertThat(route).isNotEmpty()
        }

        val titles = destinations.map { it.title }
        assertThat(titles).containsNoDuplicates()
        titles.forEach { title ->
            assertThat(title).isNotEmpty()
        }
    }

    @Test
    fun `dashboard is start destination`() {
        assertThat(Screen.Dashboard.route).isEqualTo("dashboard")
        assertThat(Screen.Dashboard.title).isEqualTo("Dashboard")
    }

    @Test
    fun `observations screen metadata is correctly defined`() {
        assertThat(Screen.Observations.route).isEqualTo("observations")
        assertThat(Screen.Observations.title).isEqualTo("Observations")
    }

    @Test
    fun `wifi screen metadata is correctly defined`() {
        assertThat(Screen.Wifi.route).isEqualTo("wifi")
        assertThat(Screen.Wifi.title).isEqualTo("Wi-Fi")
    }

    @Test
    fun `receivers screen metadata is correctly defined`() {
        assertThat(Screen.Receivers.route).isEqualTo("receivers")
        assertThat(Screen.Receivers.title).isEqualTo("Receivers")
    }

    @Test
    fun `settings screen metadata is correctly defined`() {
        assertThat(Screen.Settings.route).isEqualTo("settings")
        assertThat(Screen.Settings.title).isEqualTo("Settings")
    }
}
