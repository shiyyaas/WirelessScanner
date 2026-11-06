# WirelessScanner Project Roadmap

This roadmap defines the planned evolutionary phases for WirelessScanner. Each phase builds incrementally on the previous foundation while maintaining zero-GPL compliance, modularity, and strict observation provenance.

---

## Phase 1: Project Foundation (Completed)
- [x] Initial project scaffolding with Gradle Kotlin DSL and version catalog (`libs.versions.toml`).
- [x] Toolchain configuration: AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36.
- [x] Pure domain module `:core:model` for platform-independent immutable observation records.
- [x] Application module `:app` with Jetpack Compose, Material 3 theming, and navigation graph.
- [x] Navigation scaffold with Dashboard, Observations, Receivers, and Settings destinations.
- [x] Unit test infrastructure for domain models and UI states.
- [x] Android-suitable `.gitignore` and GitHub Actions CI workflow.
- [x] Architectural documentation, ADRs, and roadmap.

---

## Phase 2: Bluetooth Low Energy (BLE) Discovery (Completed)
- [x] Pluggable `RadioCollector` lifecycle implementation (`BleScanSessionManager`).
- [x] Android BLE scanner implementation (`AndroidBleScanner`) using `callbackFlow` and `BluetoothLeScanner`.
- [x] Android 8–11 (`ACCESS_FINE_LOCATION`) and Android 12+ (`BLUETOOTH_SCAN`) runtime permission handling with transparent `neverForLocation` omission.
- [x] Bluetooth hardware state monitoring (`BluetoothStateManager`).
- [x] Raw advertisement parsing (`BleScanResultMapper`) extracting names, UUIDs, manufacturer data, service data, Tx power, and connectability.
- [x] In-memory session aggregation (`BleDeviceAggregator`) with 30-sample history windows, rolling median RSSI, and observation counts.
- [x] BLE observation feed in `:app:ui:screens:observations` with live countdown, controls, search/filter, and expandable device cards.
- [x] Comprehensive unit tests with fake scanner implementations.

---

## Phase 3: Wi-Fi Discovery & Unified Observation Engine
- [ ] Implement Wi-Fi observation collector using Android Wi-Fi scan results (`WifiManager.getScanResults()`).
- [ ] Handle Wi-Fi permissions, scan throttling, and location service requirements.
- [ ] Normalize Wi-Fi access point observations (SSID, BSSID, RSSI, frequency, capabilities) into `Observation`.
- [ ] Implement Unified Observation Engine to merge BLE and Wi-Fi feeds while preserving source provenance.
- [ ] *Verification:* Verify scan throttling behavior on Android 10 and Android 15 (Target SDK 36).

---

## Phase 4: Local Persistence with Room
- [ ] Implement `:core:database` using Room for storing observation histories and scan sessions.
- [ ] Exclude sensitive observation databases from Android cloud backup / auto-backup.
- [ ] Provide user data controls: session naming, search, history export, and purge functionality.

---

## Phase 5: ESP32 USB Receiver Infrastructure
- [ ] Implement `:core:receiver-usb` module infrastructure for USB CDC serial communication.
- [ ] Develop basic protocol parser for ESP32-S3 JSON observations.
- [ ] Handle USB permissions and attach/detach lifecycle.
- [ ] Distinguish internal Android observations from external ESP32 observations via `ReceiverSource.ExternalUsbReceiver`.

---

## Phase 6: Signature Classification & Radar UI
- [ ] Implement explainable signature classification engine (camera makers, trackers, public safety beacons).
- [ ] Maintain an offline, permissively-licensed IEEE OUI and Bluetooth SIG lookup registry.
- [ ] Keep classifications strictly labeled as hypotheses with clear evidentiary trails.
- [ ] Introduce radar-style signal strength visualization.
