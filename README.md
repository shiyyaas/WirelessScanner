# WirelessScanner

A modern, maintainable, and privacy-preserving Android application for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, optional external USB hardware receivers). Reference receiver board: to be decided (ESP32-S3 is the planned candidate); no board is confirmed as supported. Built with Kotlin, Jetpack Compose, and Material 3.

---

## Current Status: Phase 4.5 (Compliance, Provenance & Documentation Baseline — In Progress)

This repository implements the completed phases 1 through 4 (code and tests present, CI currently failing on `:app:testDebugUnitTest`) alongside Phase 4.5 compliance hardening:
- **Phase 1 (Foundation)**: Gradle Kotlin DSL scaffolding with version catalog (`libs.versions.toml`), AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36, pure domain module `:core:model`, and navigation scaffold.
- **Phase 2 (BLE Discovery)**: `AndroidBleScanner` using `callbackFlow` and `BluetoothLeScanner`, permission handling without `neverForLocation` omission, and in-memory aggregation (`BleDeviceAggregator`). *(Code and tests present; CI currently failing on `:app:testDebugUnitTest`)*
- **Phase 3 (Wi-Fi Observation)**: `AndroidWifiScanner` using `WifiManager` scan results, throttling and location handling, and `WifiDeviceAggregator` grouped by BSSID. *(Code and tests present; CI currently failing on `:app:testDebugUnitTest`)*
- **Phase 4 (Room Persistence)**: `:core:database` module with Room persistence for scan sessions and observation histories, manifest-level backup exclusions (`allowBackup="false"`), and purge functionality. *(Code and tests present; CI currently failing on `:app:testDebugUnitTest`)*
- **Phase 4.5 (Compliance Baseline — In Progress)**: Provenance audits, CI license gating, architecture documentation sync, and limitation/privacy documentation.

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
   - Only permissively licensed dependencies (MIT, BSD, Apache-2.0) are permitted in production code. GPL/AGPL are strictly excluded.

---

## Project Structure

```
WirelessScanner/
├── .github/workflows/ci.yml # GitHub Actions CI workflow
├── core/
│   ├── model/               # Pure Kotlin JVM module (immutable observation records)
│   └── database/            # Room persistence module (scan sessions & observation histories)
├── app/                     # Android Application module
│   └── src/main/kotlin/app/wirelessscanner/
│       ├── MainActivity.kt
│       ├── collector/       # RadioCollector contracts
│       ├── ble/             # BLE Scanner, aggregator, models, and permissions
│       ├── wifi/            # Wi-Fi Discovery subsystem
│       └── ui/              # Compose theme, navigation graph, and destination screens
├── docs/                   # Architecture, Roadmap, and Decision records
│   ├── ARCHITECTURE.md     # Architectural design and module evolution
│   ├── DECISIONS.md        # Architecture Decision Records (ADRs)
│   ├── ROADMAP.md          # Milestone roadmap (Phase 1 through Phase 10)
│   ├── LIMITATIONS.md      # What the app can and cannot see
│   ├── PRIVACY.md          # Privacy policy, storage, and backup exclusions
│   ├── ALIGNMENT_REPORT.md # Verification alignment report
│   └── PROVENANCE_AUDIT.md # Provenance and dependency audit
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

> **Note on `neverForLocation`**: The app intentionally omits `neverForLocation` from `BLUETOOTH_SCAN` to prevent silent filtering of beacon signals (iBeacon, Eddystone, unassociated beacons).

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
- [Limitations](docs/LIMITATIONS.md)
- [Privacy Policy](docs/PRIVACY.md)

---

## License

WirelessScanner is licensed under the Apache License 2.0.
