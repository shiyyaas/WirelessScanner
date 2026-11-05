# WirelessScanner

A modern, maintainable, and privacy-preserving Android application for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, cellular signals and optional external USB hardware receivers). Built with Kotlin, Jetpack Compose, and Material 3.

---

## Current Status: Phase 1 (Project Foundation)

This repository currently implements **Phase 1: Project Foundation**.
- Gradle Kotlin DSL build setup using Gradle `8.11.1`, AGP `8.9.1`, and Kotlin `2.2.0`.
- Target & Compile SDK: Android 16 (API `36`); Minimum SDK: Android 8.0 (API `26`).
- Pure Kotlin JVM module `:core:model` for platform-independent domain models (`Observation`, `RadioType`, `ReceiverSource`, `SignalStrength`).
- Application module `:app` with Material 3 theming, Navigation scaffold (Dashboard, Observations, Receivers, Settings), and lifecycle contracts.
- Automated unit tests and GitHub Actions CI.
- *In accordance with Milestone 1 requirements, actual radio scanning (BLE/Wi-Fi/cellular) and external ESP32 communication are intentionally out of scope and deferred to subsequent milestones.*

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
│       ├── collector/       # RadioCollector contract (future :core:scanner-api)
│       └── ui/              # Compose theme, navigation graph, and destination screens
├── docs/
│   ├── ARCHITECTURE.md      # Architectural design and module evolution
│   ├── DECISIONS.md         # Architecture Decision Records (ADRs)
│   └── ROADMAP.md           # Milestone roadmap (Phase 1 through Phase 6)
└── gradle/
    └── libs.versions.toml   # Centralized version catalog
```

---

## Prerequisites & Environment Setup

- **JDK**: OpenJDK 21
- **Android SDK**: API 36 (`platforms;android-36`, `build-tools;35.0.0` or newer)
- **Environment Variable**: Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to your Android SDK installation (e.g. `export ANDROID_HOME=/opt/android-sdk`).
- **Gradle**: Uses included Gradle wrapper (`./gradlew`) targeting Gradle 8.11.1.

> **Note**: Do not commit machine-specific `local.properties`. Configure `ANDROID_HOME` in your environment.

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

## Documentation

- [Architecture Guide](docs/ARCHITECTURE.md)
- [Architecture Decision Records (ADRs)](docs/DECISIONS.md)
- [Development Roadmap](docs/ROADMAP.md)

---

## License

WirelessScanner is licensed under the Apache License 2.0 (or MIT). See LICENSE for details.
All third-party notices, assigned-number registries, and reference attributions are maintained in NOTICE.
