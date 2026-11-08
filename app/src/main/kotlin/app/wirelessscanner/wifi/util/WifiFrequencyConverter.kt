package app.wirelessscanner.wifi.util

import android.net.wifi.ScanResult
import app.wirelessscanner.wifi.model.WifiBand
import app.wirelessscanner.wifi.model.WifiSecurityType

object WifiFrequencyConverter {

    fun frequencyToBand(frequencyMhz: Int): WifiBand = when (frequencyMhz) {
        in 2412..2484 -> WifiBand.BAND_2_4_GHZ
        in 5150..5895 -> WifiBand.BAND_5_GHZ
        in 5925..7125 -> WifiBand.BAND_6_GHZ
        else -> WifiBand.UNKNOWN
    }

    fun frequencyToChannel(frequencyMhz: Int): Int = when (frequencyMhz) {
        in 2412..2472 -> ((frequencyMhz - 2412) / 5) + 1
        2484 -> 14
        in 5000..5900 -> (frequencyMhz - 5000) / 5
        in 5925..7125 -> {
            if (frequencyMhz == 5935) {
                2
            } else {
                ((frequencyMhz - 5950) / 5) + 1
            }
        }
        else -> 0
    }

    fun parseSecurityType(capabilities: String?): WifiSecurityType {
        if (capabilities.isNullOrBlank()) return WifiSecurityType.UNKNOWN
        val upper = capabilities.uppercase()

        val isWpa3 = upper.contains("SAE") || upper.contains("WPA3")
        val isWpa2 = upper.contains("WPA2") || (upper.contains("RSN") && !isWpa3) || (upper.contains("RSN") && upper.contains("WPA2"))
        val isWpa = upper.contains("WPA") && !upper.contains("WPA2") && !upper.contains("WPA3")
        val isOwe = upper.contains("OWE")
        val isEap = upper.contains("EAP") || upper.contains("802.1X")
        val isWep = upper.contains("WEP")
        val isPsk = upper.contains("PSK")

        // Check whether it explicitly supports both WPA2 and WPA3
        val isWpa2AndWpa3 = isWpa3 && (upper.contains("WPA2") || (upper.contains("PSK") && upper.contains("SAE")))

        return when {
            isEap -> WifiSecurityType.ENTERPRISE
            isOwe -> WifiSecurityType.OWE
            isWpa2AndWpa3 -> WifiSecurityType.WPA2_WPA3
            isWpa3 -> WifiSecurityType.WPA3
            isWpa2 && isWpa -> WifiSecurityType.WPA_WPA2
            isWpa2 -> WifiSecurityType.WPA2
            isWpa -> WifiSecurityType.WPA
            isWep -> WifiSecurityType.WEP
            !isPsk && (upper.isEmpty() || upper == "[ESS]") -> WifiSecurityType.OPEN
            else -> WifiSecurityType.UNKNOWN
        }
    }

    fun channelWidthToString(channelWidthCode: Int?): String = when (channelWidthCode) {
        ScanResult.CHANNEL_WIDTH_20MHZ -> "20 MHz"
        ScanResult.CHANNEL_WIDTH_40MHZ -> "40 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ -> "80 MHz"
        ScanResult.CHANNEL_WIDTH_160MHZ -> "160 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
        5 -> "320 MHz"
        else -> "Unknown"
    }
}
