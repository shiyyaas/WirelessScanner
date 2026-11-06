# WirelessScanner — Development Roadmap

## Phase 3 to Full Project Completion

*Project:* WirelessScanner  
*Platform:* Android  
*Language:* Kotlin  
*UI:* Jetpack Compose + Material 3  
*Architecture:* Modular, clean, offline-first  
*Minimum Android:* API 26  
*Target SDK:* 36  
*JDK:* 21  

---

# 1. Current Project Status

## Phase 1 — Project Foundation
*Status: COMPLETED*

Implemented:
•  Kotlin Android project foundation.
•  Jetpack Compose and Material 3.
•  Gradle Kotlin DSL and version catalog.
•  :app module.
•  :core:model pure Kotlin module.
•  Basic navigation.
•  Unit testing foundation.
•  GitHub repository.
•  GitHub Actions CI workflow.
•  Project documentation.

## Phase 2 — Bluetooth Low Energy Discovery
*Status: IMPLEMENTED AND MERGED*

Implemented:
•  BLE discovery using Android Bluetooth APIs.
•  BLE scanning using callbackFlow.
•  User-controlled 15-second scanning sessions.
•  Automatic scan cancellation.
•  Bluetooth state handling.
•  Android BLE permission handling.
•  Advertisement data parsing.
•  Device aggregation by BLE address.
•  Rolling median RSSI calculation.
•  RSSI history.
•  Material 3 Compose discovery screen.
•  Sorting and filtering.
•  Device detail cards.
•  Unit tests.

Physical-device validation should be completed on the Vivo Y30 running Android 10.

## GitHub CI
Existing workflow:
.github/workflows/ci.yml

Current responsibilities:
•  Checkout repository.
•  Set up JDK 21.
•  Set up Gradle.
•  Run core model tests.
•  Run app unit tests.
•  Compile Debug APK.

Planned improvement:
•  Upload generated APK as a GitHub Actions artifact.
•  Support manual workflow execution.
•  Keep test failures from producing a successful build.
•  Make CI a required check before merging pull requests.

---

# 2. Phase 3 — Wi-Fi Access Point Discovery & Unified Observation Engine

*Goal:* Implement passive Wi-Fi access point discovery using standard Android Wi-Fi APIs and aggregate with BLE data.

## Core implementation
Use:
•  WifiManager.
•  ScanResult.
•  BroadcastReceiver.
•  Kotlin Coroutines / Flow.

Create a Wi-Fi collector, mapper, and the Unified Observation Engine.

Suggested architecture:

:core:model
    |
    |-- Observation (Unified)
    |-- RadioType
    |-- SignalStrength
    |
:app
    |
    |-- collector/
    |   |-- RadioCollector (Interface)
    |   |-- BleCollector
    |   |-- WifiCollector
    |   |-- UnifiedObservationEngine (Aggregator)
    |
    |-- wifi/ (Wifi-specific logic)
    |   |-- AndroidWifiScanner
    |   |-- WifiPermissionManager
    |   |-- WifiResultMapper
    |

## Permissions
Handle Android versions correctly.
•  `NEARBY_WIFI_DEVICES` (Android 13+) with `neverForLocation` (Note: `ACCESS_FINE_LOCATION` still strongly recommended for hardware results).
•  `ACCESS_FINE_LOCATION` (Android 10-12).

## Unified Observation Engine
•  Combine BLE and Wi-Fi feeds.
•  Preserve original radio source.
•  Normalize data for UI.

---

# 3. Phase 4 — Local Persistence with Room

*Goal:* Make WirelessScanner capable of retaining observations across app sessions.

## Technology
•  Room Database.
•  Repository pattern.

## Features
•  Persist discovered observations.
•  Store RSSI history.
•  Search historical observations.
•  Export CSV/JSON.

---

# 4. Phase 5 — Optional External Hardware Support

*Goal:* Add optional external radio observation hardware (USB-Serial).

## Core implementation
•  USB Serial communication layer.
•  JSON protocol parser.
•  Distinguish via `ReceiverSource.ExternalUsbReceiver`.

---

# 5. Phase 6 — Device Signature & Classification Engine

*Goal:* Help users understand what kind of radio device they are observing using rules (not claims).

## Features
•  Rule-based classification.
•  Offline signature database.
•  Confidence scoring.
•  Radar-style visualization (Non-positional).

---

# 6. Future Phases
•  Phase 7+: Radar/Investigation Interface, Cellular info, Production Optimization.

---

# 7. Development Rules for Every Future Phase
1. Inspect the existing repository before making changes.
2. Read AGENTS.md and the architecture documentation.
3. Do not rewrite working code unnecessarily.
4. Keep the domain model platform-independent.
5. Write unit tests for new functionality.
6. Run all relevant tests before completing a phase.
