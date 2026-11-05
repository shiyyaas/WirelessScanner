# Architecture Decision Records (ADRs)

## ADR 001: Reference Projects License and Reusable Architecture Review

### Context
We evaluated three reference open-source wireless observation projects for architecture and design patterns:
1. **Fieldwatch** (`https://github.com/OffGridPete/Fieldwatch`):
   - **License**: MIT License (`Copyright (c) 2026 Off Grid Pete LLC`). AndroidX/Kotlin libraries are Apache-2.0. Third-party datasets (IEEE OUI, Bluetooth SIG) have separate notices.
   - **Architecture**: Single `:app` module, passive observation focus, signature library, pairing flood detection, GNSS interference, cell connection monitoring.
   - **Takeaways**: Strong focus on passive-only operation, offline operation, avoiding false identity claims, and clear privacy considerations.
2. **BLE-Device-Radar** (`https://github.com/necdetzr/BLE-Device-Radar`):
   - **License**: Apache License 2.0 (`necdetzr`).
   - **Architecture**: Multi-module Now-in-Android style architecture (`:app`, `:core:ble`, `:core:data`, `:core:database`, `:core:model`, `:feature:radar`, etc.).
   - **Takeaways**: Clean Unidirectional Data Flow (UDF), Compose Material 3 implementation, version catalog usage (`libs.versions.toml`), clear repository contracts.
3. **MILLIE** (`https://github.com/IDEAGREY/MILLIE`):
   - **License**: MIT License (`idea grey`).
   - **Architecture**: Separated Android application and ESP32 CYD USB firmware (`millie2/android` and `millie2/firmware`).
   - **Takeaways**: Distinct observation tracking between internal phone scanning and external CYD USB scanning. Preserves distinct receiver evidence while deduplicating matching wireless identifiers in UI.

### Decision
- All reference projects use permissive licenses (MIT or Apache 2.0). **Zero GPL-licensed code or dependencies will be introduced.**
- Any third-party datasets or notices incorporated in future milestones (e.g. IEEE OUI registries or Bluetooth SIG company IDs) must preserve original copyrights, license terms, and notices in a dedicated `NOTICE` file.
- Reusable architectural concepts adopted:
  - Clean separation of pure domain models from Android platform collectors (inspired by BLE-Device-Radar).
  - Explicit provenance tagging distinguishing internal Android radios from external USB receivers (inspired by MILLIE).
  - Passive-only observation philosophy without active intervention or identity fabrication (inspired by Fieldwatch).

### Status
Accepted.

---

## ADR 002: Toolchain Alignment for Android 16 (API 36) Compatibility

### Context
The project foundation requires modern, stable, and mutually compatible tooling for Android development targeting API 36 (compileSdk 36, targetSdk 36).
We evaluated Gradle, Android Gradle Plugin (AGP), Kotlin, and JDK versions available in the Jules environment.

### Decision
- **Gradle**: `8.11.1` (supported by the Android toolchain and AGP 8.9.x).
- **Android Gradle Plugin (AGP)**: `8.9.1`.
- **Kotlin**: `2.2.0` with the Kotlin Compose Compiler Gradle plugin `2.2.0`.
- **JDK**: OpenJDK 21 (Ubuntu `21.0.10+7`).
- **Android SDK**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (Android 8.0 Oreo minimum for BLE/Wi-Fi scan APIs).
- **Environment**: Use standard `ANDROID_HOME` without committing any machine-specific `local.properties`.

### Status
Accepted.

---

## ADR 003: Pure Kotlin JVM `:core:model` Module

### Context
Domain models (`RadioType`, `Observation`, `SignalStrength`, `ReceiverSource`) should not depend on the Android framework. Tying models to Android SDK classes (such as `android.bluetooth.le.ScanResult` or `android.os.Parcelable`) impairs portability, complicates testing, and creates circular dependencies.

### Decision
- `:core:model` is configured as a pure Kotlin JVM library (`plugins { kotlin("jvm") }`).
- Domain models are strictly platform-independent.
- Payloads are stored immutably (`List<Byte>`) to prevent mutation leaks across threads or components.
- Scanner abstractions and Android-specific implementations are kept out of `:core:model`. A temporary `RadioCollector` interface resides in `:app` and will migrate to `:core:scanner-api` in Milestone 2.

### Status
Accepted.
