# WirelessScanner Limitations & Operational Boundaries

WirelessScanner is designed for lawful, passive observation of nearby wireless signals. Understanding its technical boundaries and operating system constraints is essential for interpreting observations correctly.

---

## 1. What WirelessScanner Can See
- **BLE Advertisements**: Nearby Bluetooth Low Energy broadcast packets (advertisements, scan responses, service UUIDs, manufacturer data, Tx power) from active broadcasting peripherals.
- **Wi-Fi Access Points**: Nearby Wi-Fi access point broadcasts (SSID, BSSID, RSSI, operating frequency/channel, security classifications) detected via standard Android Wi-Fi scan results.
- **Signal Strength (RSSI)**: Raw received signal strength indication in dBm, aggregated into rolling medians and 30-sample history windows.

---

## 2. What WirelessScanner Cannot See (Non-Goals & Limitations)
- **No Monitor Mode / Packet Capture**: Android platform APIs do not support raw 802.11 monitor mode, promiscuous mode, or packet capture of arbitrary frames.
- **No Packet Injection or Deauthentication**: The app performs passive observation only. It never transmits frames, deauthenticates clients, or interacts actively with wireless devices.
- **No Bluetooth Classic Discovery**: Scans are restricted to BLE (Bluetooth Low Energy) advertising packets. Classic Bluetooth connections, paired audio devices, or BR/EDR discovery are not scanned or displayed.
- **No Probe-Only Client Tracking**: Wi-Fi scanning observes access points (`WifiManager.getScanResults()`) rather than disassociated client probe requests. Unassociated client stations probing for hidden or remembered networks are not tracked.
- **No Direction Finding or Physical Distance Estimation**: RSSI alone fluctuates due to multipath propagation, reflection, body absorption, and antenna orientation. **RSSI is never converted into distance (meters) or physical direction (bearing).**
- **Address Randomization**: Modern mobile devices (smartphones, tablets, wearables) use MAC address randomization (Local Administration Bit set) that rotates identifiers periodically. WirelessScanner treats each randomized address as a distinct observation and never attempts to link or de-anonymize rotating identifiers.
- **Scan Throttling**: Android operating system policies limit scan frequencies:
  - **Wi-Fi Scan Throttling**: Android 9+ limits foreground applications to roughly 4 scans per 2-minute window.
  - **BLE Scan Limits**: Android OS limits continuous BLE scan starts to prevent battery drain.
  - When scans are throttled or rate-limited by the OS, cached results are displayed with appropriate freshness indicators.

---

## 3. Platform & Hardware Transparency
The application operates entirely within the boundaries defined by official Android platform APIs. When scan results are cached, rate-limited, or restricted by OS permissions, the user interface indicates the current status explicitly.
