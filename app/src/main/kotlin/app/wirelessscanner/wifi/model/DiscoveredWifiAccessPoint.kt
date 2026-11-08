package app.wirelessscanner.wifi.model

import app.wirelessscanner.core.model.SignalStrength

enum class WifiBand(val displayName: String) {
    BAND_2_4_GHZ("2.4 GHz"),
    BAND_5_GHZ("5 GHz"),
    BAND_6_GHZ("6 GHz"),
    UNKNOWN("Unknown");

    override fun toString(): String = displayName
}

enum class WifiSecurityType(val displayName: String) {
    OPEN("Open / None"),
    WEP("WEP"),
    WPA("WPA-Personal"),
    WPA2("WPA2-Personal"),
    WPA3("WPA3-Personal"),
    WPA_WPA2("WPA/WPA2-Personal"),
    WPA2_WPA3("WPA2/WPA3-Personal"),
    OWE("Enhanced Open (OWE)"),
    ENTERPRISE("Enterprise (802.1X)"),
    UNKNOWN("Unknown");

    override fun toString(): String = displayName
}

data class DiscoveredWifiAccessPoint(
    val bssid: String,
    val ssid: String? = null,
    val latestRssi: SignalStrength,
    val medianRssi: Int,
    val rssiHistory: List<Int> = emptyList(),
    val frequencyMhz: Int,
    val channel: Int,
    val band: WifiBand,
    val channelWidth: String,
    val securityType: WifiSecurityType,
    val capabilities: String,
    val firstSeenEpochMillis: Long,
    val lastSeenEpochMillis: Long,
    val observationCount: Int = 1,
    val isFresh: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
) {
    init {
        require(bssid.isNotBlank()) { "bssid must not be blank" }
        require(firstSeenEpochMillis > 0) { "firstSeenEpochMillis must be positive" }
        require(lastSeenEpochMillis >= firstSeenEpochMillis) {
            "lastSeenEpochMillis ($lastSeenEpochMillis) cannot be earlier than firstSeenEpochMillis ($firstSeenEpochMillis)"
        }
        require(observationCount >= 1) { "observationCount must be at least 1" }
    }

    val displaySsid: String
        get() = if (ssid.isNullOrBlank() || ssid == "<unknown ssid>") "<Hidden SSID>" else ssid

    val isHiddenSsid: Boolean
        get() = ssid.isNullOrBlank() || ssid == "<unknown ssid>"
}
