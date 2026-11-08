package app.wirelessscanner.wifi.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType

class WifiFrequencyConverterTest {

    @Test
    fun frequencyToBand_classifiesBandsCorrectly() {
        // 2.4 GHz
        assertThat(WifiFrequencyConverter.frequencyToBand(2412)).isEqualTo(WifiBand.BAND_2_4_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(2462)).isEqualTo(WifiBand.BAND_2_4_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(2484)).isEqualTo(WifiBand.BAND_2_4_GHZ)

        // 5 GHz
        assertThat(WifiFrequencyConverter.frequencyToBand(5180)).isEqualTo(WifiBand.BAND_5_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(5500)).isEqualTo(WifiBand.BAND_5_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(5825)).isEqualTo(WifiBand.BAND_5_GHZ)

        // 6 GHz
        assertThat(WifiFrequencyConverter.frequencyToBand(5935)).isEqualTo(WifiBand.BAND_6_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(6105)).isEqualTo(WifiBand.BAND_6_GHZ)
        assertThat(WifiFrequencyConverter.frequencyToBand(7115)).isEqualTo(WifiBand.BAND_6_GHZ)

        // Unknown
        assertThat(WifiFrequencyConverter.frequencyToBand(900)).isEqualTo(WifiBand.UNKNOWN)
        assertThat(WifiFrequencyConverter.frequencyToBand(3500)).isEqualTo(WifiBand.UNKNOWN)
    }

    @Test
    fun frequencyToChannel_calculatesStandardChannels() {
        // 2.4 GHz
        assertThat(WifiFrequencyConverter.frequencyToChannel(2412)).isEqualTo(1)
        assertThat(WifiFrequencyConverter.frequencyToChannel(2437)).isEqualTo(6)
        assertThat(WifiFrequencyConverter.frequencyToChannel(2462)).isEqualTo(11)
        assertThat(WifiFrequencyConverter.frequencyToChannel(2472)).isEqualTo(13)
        assertThat(WifiFrequencyConverter.frequencyToChannel(2484)).isEqualTo(14)

        // 5 GHz
        assertThat(WifiFrequencyConverter.frequencyToChannel(5180)).isEqualTo(36)
        assertThat(WifiFrequencyConverter.frequencyToChannel(5200)).isEqualTo(40)
        assertThat(WifiFrequencyConverter.frequencyToChannel(5745)).isEqualTo(149)

        // 6 GHz
        assertThat(WifiFrequencyConverter.frequencyToChannel(5955)).isEqualTo(2)
        assertThat(WifiFrequencyConverter.frequencyToChannel(5975)).isEqualTo(6)

        // Out of band
        assertThat(WifiFrequencyConverter.frequencyToChannel(1000)).isEqualTo(0)
    }

    @Test
    fun parseSecurityType_identifiesSecurityProtocols() {
        assertThat(WifiFrequencyConverter.parseSecurityType("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))
            .isEqualTo(WifiSecurityType.WPA2)

        assertThat(WifiFrequencyConverter.parseSecurityType("[RSN-SAE-CCMP][ESS]"))
            .isEqualTo(WifiSecurityType.WPA3)

        assertThat(WifiFrequencyConverter.parseSecurityType("[RSN-PSK+SAE-CCMP][ESS]"))
            .isEqualTo(WifiSecurityType.WPA2_WPA3)

        assertThat(WifiFrequencyConverter.parseSecurityType("[WPA-PSK-CCMP+TKIP][RSN-PSK-CCMP+TKIP][ESS]"))
            .isEqualTo(WifiSecurityType.WPA_WPA2)

        assertThat(WifiFrequencyConverter.parseSecurityType("[WEP][ESS]"))
            .isEqualTo(WifiSecurityType.WEP)

        assertThat(WifiFrequencyConverter.parseSecurityType("[RSN-EAP/SHA1-CCMP][ESS]"))
            .isEqualTo(WifiSecurityType.ENTERPRISE)

        assertThat(WifiFrequencyConverter.parseSecurityType("[OWE-CCMP][ESS]"))
            .isEqualTo(WifiSecurityType.OWE)

        assertThat(WifiFrequencyConverter.parseSecurityType("[ESS]"))
            .isEqualTo(WifiSecurityType.OPEN)

        assertThat(WifiFrequencyConverter.parseSecurityType(""))
            .isEqualTo(WifiSecurityType.UNKNOWN)

        assertThat(WifiFrequencyConverter.parseSecurityType(null))
            .isEqualTo(WifiSecurityType.UNKNOWN)
    }
}
