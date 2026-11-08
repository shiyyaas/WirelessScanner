package app.wirelessscanner.wifi.scanner

import app.wirelessscanner.core.model.RadioType
import app.wirelessscanner.core.model.ReceiverSource
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class WifiScanResultMapperTest {

    @Test
    fun normalizeBssid_handlesNullAndBlank() {
        val method = WifiScanResultMapper.javaClass.getDeclaredMethod("normalizeBssid", String::class.java)
        method.isAccessible = true

        assertThat(method.invoke(WifiScanResultMapper, null)).isEqualTo("00:00:00:00:00:00")
        assertThat(method.invoke(WifiScanResultMapper, "")).isEqualTo("00:00:00:00:00:00")
        assertThat(method.invoke(WifiScanResultMapper, "aa:bb:cc:dd:ee:ff")).isEqualTo("AA:BB:CC:DD:EE:FF")
    }

    @Test
    fun normalizeSsid_handlesHiddenAndQuotes() {
        val method = WifiScanResultMapper.javaClass.getDeclaredMethod("normalizeSsid", String::class.java)
        method.isAccessible = true

        assertThat(method.invoke(WifiScanResultMapper, null)).isNull()
        assertThat(method.invoke(WifiScanResultMapper, "")).isNull()
        assertThat(method.invoke(WifiScanResultMapper, "<unknown ssid>")).isNull()
        assertThat(method.invoke(WifiScanResultMapper, "\"MyHomeWifi\"")).isEqualTo("MyHomeWifi")
        assertThat(method.invoke(WifiScanResultMapper, "GuestNetwork")).isEqualTo("GuestNetwork")
    }
}
