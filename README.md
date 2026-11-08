# WirelessScanner

A modern, maintainable, and privacy-preserving Android application for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, cellular signals and optional external USB hardware receivers such as ESP32-S3). Built with Kotlin, Jetpack Compose, and Material 3.

---

## Current Status: Phase 4.5 (Compliance, Provenance & Documentation Baseline — In Progress)

This repository implements **Phase 4 (Local Persistence with Room)** alongside completed Phase 1 (Foundation), Phase 2 (BLE Discovery), and Phase 3 (Wi-Fi Discovery):
- **Phase 1: Project Foundation** — Gradle Kotlin DSL, version catalog (`libs.versions.toml`), AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36, `:core:model` pure domain module, Jetpack Compose UI scaffold, and CI workflow.
- **Phase 2: Bluetooth Low Energy (BLE) Discovery** — `AndroidBleScanner` using `callbackFlow` and `BluetoothLeScanner`, `RadioCollector` lifecycle (`BleScanSessionManager`), permission handling without `neverForLocation` omission (ADR 004), advertisement parsing (`AndroidBleResultMapper`), and in-memory aggregation (`BleDeviceAggregator`).
- **Phase 3: Android Wi-Fi Observation** — `AndroidWifiScanner` using `WifiManager` scan results, scan-throttling awareness, frequency conversion (2.4, 5, 6 GHz), security parsing, and in-memory BSSID grouping (`WifiDeviceAggregator`).
- **Phase 4: Local Persistence with Room** — `:core:database` module storing scan sessions and observation histories securely in local storage with backup exclusion rules.
- **Phase 4.5: Compliance & Documentation Baseline** — Current milestone establishing license gates, provenance audits, and documentation synchronization.

---

## Architectural Principles & Provenance

1. **Strict Passive Observation**: The application only observes signals passively received over the air or via attached receivers. It performs no network intrusion, packet injection, deauthentication, or unauthorized access.
2. **Observation Provenance**:
   - Every observation records its origin via `ReceiverSource` (`Internal` vs `ExternalUsbReceiver`).
   - Accurate timestamps, raw signal strength (dBm), and raw broadcast payloads (stored immutably as `List<Byte>`) are recorded.
   - Matching MAC addresses, BSSIDs, or names across radios or time are **not** assumed to represent the same physical device.
   - Device classifications are hypotheses supported by explicit evidence trails, not ground truth.
3. **Hardware Agnosticism**:
   - External hardware is completely optional. The app functions standalone using the phone's internal sensors.
4. **Permissive Licensing & Zero GPL**:
   - Only permissively licensed dependencies (MIT, BSD, Apache-2.0) are permitted. GPL/AGPL are strictly excluded.

---

## Project Structure

```
WirelessScanner/
├── .github/workflows/ci.yml # GitHub Actions CI workflow
├── core/
│   ├── model/               # Pure Kotlin JVM module (immutable observation records)
│   └── database/            # Room persistence module (scan sessions & observations)
├── app/                     # Android Application module
│   └── src/main/kotlin/app/wirelessscanner/
│       ├── MainActivity.kt
│       ├── collector/       # RadioCollector contracts
│       ├── ble/             # BLE Scanner, aggregator, models, and permissions
│       ├── wifi/            # Wi-Fi Discovery subsystem
│       └── ui/              # Compose theme, navigation graph, and destination screens
│           ├── dashboard/   # Overview dashboard
│           ├── observations/# BLE observation feed & ViewModel
│           ├── wifi/        # Wi-Fi discovery feed & ViewModel
│           ├── receivers/   # Hardware & receiver status
│           ├── settings/    # App settings & preferences
│           └── navigation/  # NavHost and Destination graph
├── docs/
│   ├── ARCHITECTURE.md      # Architectural design and module evolution
│   ├── DECISIONS.md         # Architecture Decision Records (ADRs)
│   ├── ROADMAP.md           # Milestone roadmap
│   ├── ALIGNMENT_REPORT.md  # Verification & roadmap alignment report
│   ├── PROVENANCE_AUDIT.md  # Dependency licence & provenance audit
│   ├── LIMITATIONS.md       # User-facing app capabilities and limitations
│   └── PRIVACY.md           # Privacy architecture and backup exclusion
└── gradle/
    └── libs.versions.toml   # Centralized version catalog
```

---

## Android Permissions Matrix

| Android Version | API Level | Required Runtime Permission | Manifest Install Permissions | Purpose |
|---|---|---|---|---|
| Android 8.0 – 11 | API 26–30 | `ACCESS_FINE_LOCATION` | `BLUETOOTH`, `BLUETOOTH_ADMIN`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | Required by Android OS to deliver BLE and Wi-Fi scan results. |
| Android 12 | API 31–32 | `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION` | `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | `BLUETOOTH_SCAN` for BLE; `ACCESS_FINE_LOCATION` for Wi-Fi discovery. |
| Android 13 – 16 | API 33–36 | `BLUETOOTH_SCAN`, `ACCESS_FINE_LOCATION`, `NEARBY_WIFI_DEVICES` | `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE` | Dedicated permissions for nearby wireless discovery. |

> **Note on `neverForLocation`**: The app intentionally omits `neverForLocation` from `BLUETOOTH_SCAN` per ADR 004 to ensure beacon signals (iBeacon, Eddystone) are not silently filtered by the OS.

---

## Build & Test Commands

To run all builds and tests using the Gradle wrapper:

```bash
# 1. Run unit tests for core domain models
./gradlew :core:model:test

# 2. Run unit tests for the Android application module
./gradlew :app:testDebugUnitTest

# 3. Assemble the debug APK
./gradlew :app:assembleDebug
```

---

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Architecture Decision Records (ADRs)](docs/DECISIONS.md)
- [Development Roadmap](docs/ROADMAP.md)
- [Alignment Report](docs/ALIGNMENT_REPORT.md)
- [Provenance Audit](docs/PROVENANCE_AUDIT.md)
- [Limitations](docs/LIMITATIONS.md)
- [Privacy Policy & Storage](docs/PRIVACY.md)

---

## License

WirelessScanner is licensed under the Apache License 2.0. See LICENSE for details.
