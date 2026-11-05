# WirelessScanner

A modern, maintainable, and privacy-preserving Android application for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, cellular signals and optional external USB hardware receivers). Built with Kotlin, Jetpack Compose, and Material 3.

---

## Current Status: Phase 2 (Bluetooth Low Energy Discovery)

This repository implements **Phase 2: Bluetooth Low Energy (BLE) Discovery**:
- Android Bluetooth Low Energy scanning via Kotlin Coroutines and `callbackFlow` wrapped inside `AndroidBleScanner`.
- Controlled foreground scan sessions (`BleScanSessionManager`) with configurable 15-second default duration, duplicate scan prevention, countdown timer, and automatic timeout.
- Android permission management supporting both Android 8–11 (API 26–30: `ACCESS_FINE_LOCATION`) and Android 12+ (API 31+: `BLUETOOTH_SCAN`).
- Intentional omission of `neverForLocation` to prevent platform-level filtering of BLE beacons and unassociated devices.
- In-memory aggregation (`BleDeviceAggregator`) tracking first seen / last seen timestamps, observation counts, latest RSSI, 30-sample RSSI history windows, and median RSSI calculations.
- Material 3 user interface displaying live discovery status, device counts, RSSI categorical indicators, search/filter controls, and expandable advertisement detail sheets.
- Preserved byte payload immutability (`List<Byte>`), keeping `:core:model` free from Android dependencies.
- Automated unit test suite with fake scanner implementations.

---

## Architectural Principles & Provenance

1. **Strict Passive Observation**: The application only observes signals passively received over the air or via attached receivers. It performs no network intrusion, packet injection, deauthentication, or unauthorized access.
2. **Observation Provenance**:
   - Every observation records its origin via `ReceiverSource` (distinguishing internal device radios from external USB receivers like ESP32 CYD).
   - Accurate timestamps, raw signal strength (dBm), and raw broadcast payloads (stored immutably) are recorded.
   - Matching MAC addresses, BSSIDs, or names across radios or time are **not** assumed to represent the same physical device.
   - Device classifications are hypotheses supported by explicit evidence trails, not ground truth.
3. **Hardware Agnosticism**:
   - External hardware is completely optional. The app functions standalone using the phone's internal sensors.
4. **Permissive Licensing & Zero GPL**:
   - No GPL-licensed code or dependencies are permitted. Reference projects (Fieldwatch: MIT, BLE-Device-Radar: Apache-2.0, MILLIE: MIT) are permissively licensed, and all third-party notices are strictly preserved.

---

## Project Structure

```
WirelessScanner/
├── .github/workflows/ci.yml # GitHub Actions CI workflow
├── core/
│   └── model/               # Pure Kotlin JVM module (immutable observation records)
│       └── src/main/kotlin/app/wirelessscanner/core/model/
├── app/                     # Android Application module
│   └── src/main/kotlin/app/wirelessscanner/
│       ├── MainActivity.kt
│       ├── collector/       # RadioCollector contracts
│       ├── ble/             # BLE Scanner, aggregator, models, and permissions
│       │   ├── aggregator/  # BleDeviceAggregator (rolling median, count, history)
│       │   ├── model/       # DiscoveredBleDevice domain model
│       │   ├── permission/  # BlePermissionManager & BluetoothStateManager
│       │   └── scanner/     # AndroidBleScanner, BleScanSessionManager, BleScanResultMapper
│       └── ui/              # Compose theme, navigation graph, and destination screens
│           └── screens/observations/ # Observation feed, controls & ViewModel
├── docs/
│   ├── ARCHITECTURE.md      # Architectural design and module evolution
│   ├── DECISIONS.md         # Architecture Decision Records (ADRs)
│   └── ROADMAP.md           # Milestone roadmap (Phase 1 through Phase 6)
└── gradle/
    └── libs.versions.toml   # Centralized version catalog
```

---

## Android Permissions Matrix

| Android Version | API Level | Required Runtime Permission | Manifest Install Permissions | Purpose |
|---|---|---|---|---|
| Android 8.0 – 11 | API 26–30 | `ACCESS_FINE_LOCATION` | `BLUETOOTH`, `BLUETOOTH_ADMIN` | Required by Android OS to deliver BLE scan results. |
| Android 12 – 16 | API 31–36 | `BLUETOOTH_SCAN` | None | Dedicated permission for discovering nearby BLE peripherals. |

> **Note on `neverForLocation`**: The app intentionally omits `neverForLocation` from `BLUETOOTH_SCAN`. Android documentation states that asserting `neverForLocation` prevents discovery of beacon signals (e.g. iBeacon, Eddystone, and unassociated beacons). Since WirelessScanner is designed for transparent passive signal observation, full signal visibility is preserved.

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

# Run all checks and builds in sequence
./gradlew clean :core:model:test :app:testDebugUnitTest :app:assembleDebug
```

The assembled debug APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## Manual Physical Device Testing Guide

1. Deploy the debug APK to a physical Android device (`adb install -r app/build/outputs/apk/debug/app-debug.apk`).
2. Launch **WirelessScanner** and navigate to the **Observations** tab.
3. If Bluetooth is turned off, verify that the **Bluetooth Disabled** alert banner appears.
4. Enable Bluetooth on the device and verify that the banner updates.
5. Tap **Scan (15s)**. Verify that the runtime permission prompt appears:
   - On Android 8–11: Location permission prompt.
   - On Android 12+: Nearby Devices permission prompt.
6. Grant permission and verify that the progress bar counts down from 15 seconds.
7. Verify that nearby BLE devices populate the list with:
   - Advertised name (or "Unnamed BLE Device").
   - MAC address in monospace format.
   - Signal strength category badge (dBm).
   - First observed and last observed timestamps.
8. Tap a device card to expand and review parsed advertisement metadata (Service UUIDs, Manufacturer Data, Tx Power, connectability, raw payload hex).
9. Test sorting chips (Signal, Recent, Count) and filtering chips (All, Named Only) as well as the search bar.
10. Verify that after 15 seconds, scanning halts automatically and the Stop button reverts to "Scan (15s)".

---

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Architecture Decision Records (ADRs)](docs/DECISIONS.md)
- [Development Roadmap](docs/ROADMAP.md)

---

## License

WirelessScanner is licensed under the Apache License 2.0 (or MIT). See LICENSE for details.
All third-party notices, assigned-number registries, and reference attributions are maintained in NOTICE.
