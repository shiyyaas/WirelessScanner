# WirelessScanner Project Roadmap

This roadmap defines the planned evolutionary phases for WirelessScanner. Each phase builds incrementally on the previous foundation while maintaining zero-GPL compliance, modularity, and strict observation provenance.

---

## Phase 1: Project Foundation (Current Phase)
- [x] Initial project scaffolding with Gradle Kotlin DSL and version catalog (`libs.versions.toml`).
- [x] Toolchain configuration: AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36.
- [x] Pure domain module `:core:model` for platform-independent immutable observation records.
- [x] Application module `:app` with Jetpack Compose, Material 3 theming, and navigation graph.
- [x] Navigation scaffold with Dashboard, Observations, Receivers, and Settings destinations.
- [x] Unit test infrastructure for domain models and UI states.
- [x] Android-suitable `.gitignore` and GitHub Actions CI workflow.
- [x] Architectural documentation, ADRs, and roadmap.
- *Strict restriction: No actual BLE, Wi-Fi, cellular, or USB radio scanning in this phase.*

---

## Phase 2: Radio Collector Architecture & Android BLE Observation
- [ ] Create `:core:scanner-api` module defining pluggable `RadioCollector` lifecycle, scan configurations, and status state flows.
- [ ] Implement `:core:ble` Android BLE observation module using `android.bluetooth.le`.
- [ ] Implement Android 12+ runtime permissions handling (`BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`).
- [ ] Handle Bluetooth hardware states (disabled, unsupported, throttling).
- [ ] Raw advertisement parsing (UUIDs, manufacturer data, Tx power) normalized into `Observation`.
- [ ] BLE observation feed in `:app:ui:observations`.

---

## Phase 3: Android Wi-Fi Observation & Deduplication Feed
- [ ] Implement `:core:wifi` module using Android Wi-Fi scan results (`WifiManager.getScanResults()`).
- [ ] Handle Wi-Fi permissions, scan throttling, and location service requirements.
- [ ] Normalize Wi-Fi access point observations (SSID, BSSID, RSSI, frequency, capabilities) into `Observation`.
- [ ] Real-time observation deduplication feed presenting multi-radio signals while preserving source provenance.

---

## Phase 4: Local Persistence with Room
- [ ] Implement `:core:database` using Room for storing observation histories and scan sessions.
- [ ] Exclude sensitive observation databases from Android cloud backup / auto-backup.
- [ ] Provide user data controls: session naming, search, history export, and purge functionality.

---

## Phase 5: Optional External ESP32 USB Receiver Support
- [ ] Implement `:core:receiver-usb` module using Android USB Host API / USB CDC serial driver.
- [ ] Connect with external ESP32 microcontrollers (e.g. CYD / ESP32-S3).
- [ ] Handle USB permissions, hot-plug attach/detach lifecycle, and baud rate negotiation.
- [ ] Distinguish internal Android observations from external ESP32 observations via `ReceiverSource.ExternalUsbReceiver`.

---

## Phase 6: Signature Classification & Radar UI
- [ ] Implement explainable signature classification engine (camera makers, trackers, public safety beacons).
- [ ] Maintain an offline, permissively-licensed IEEE OUI and Bluetooth SIG lookup registry.
- [ ] Keep classifications strictly labeled as hypotheses with clear evidentiary trails.
- [ ] Introduce radar-style signal strength visualization.
