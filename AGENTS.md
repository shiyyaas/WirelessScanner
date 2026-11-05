# WirelessScanner — Agent Instructions

## Project Goal

Build a modular Android application for observing nearby wireless signals using Android-supported APIs, with optional external receiver hardware in future versions.

The app is intended for lawful, passive observation and analysis. Do not implement intrusion, exploitation, credential collection, unauthorized access, or interference with wireless devices.

---

## Technology Stack

• **Language**: Kotlin 2.2.0
• **Tooling**: Gradle 8.11.1, Android Gradle Plugin (AGP) 8.9.1, JDK 21
• **Target Platform**: `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26` (Android 8.0 Oreo)
• **UI Framework**: Jetpack Compose with Material 3 and Compose Navigation
• **Build DSL**: Gradle Kotlin DSL (`build.gradle.kts`, `settings.gradle.kts`)
• **Dependency Management**: Gradle Version Catalog (`gradle/libs.versions.toml`)
• **Testing**: JUnit 4, Google Truth, Kotlinx Coroutines Test

---

## Architecture Principles

1. **Keep radio-specific collectors separate**: Keep protocol parsers out of core data models.
2. **Normalize into shared observation models**: Core models (`:core:model`) must remain pure Kotlin JVM without Android framework dependencies.
3. **Observation & Data Integrity**:
   - Preserve original source (`ReceiverSource`), accurate epoch timestamps, and raw payloads.
   - Payloads must use immutable structures (`List<Byte>`), not mutable arrays.
   - Distinguish advertised device names from cached system names.
   - Do not assume matching addresses or names represent the same physical device.
   - Treat device classifications as hypotheses backed by explicit evidence trails, not facts.
   - Never claim distance or direction based solely on RSSI.
4. **Make external hardware optional**: The application functions standalone using internal radios; external USB peripherals (e.g. ESP32 CYD) are strictly optional enhancements.
5. **Prefer small, testable modules**: Avoid monolithic dumping grounds and empty modules without purpose.

---

## Licensing & Compliance

- Reference projects may be studied for architecture and design patterns (Fieldwatch: MIT, BLE-Device-Radar: Apache-2.0, MILLIE: MIT).
- **Strictly forbidden**: Introducing any GPL-licensed code or dependencies into this repository.
- Preserve all copyright notices, license headers, and third-party notices.

---

## Development & Build Rules

1. **Verify Environment**: Rely on `ANDROID_HOME` or system SDK paths. **Never commit `local.properties` or hardcoded environment paths.**
2. **Explicit Verification**: Always run and report real results for:
   - `./gradlew :core:model:test`
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:assembleDebug`
3. **No Unimplemented Radios in Phase 1**: Actual BLE scanning, Wi-Fi scanning, cellular scanning, USB communication, and signature classification are strictly deferred to future milestones.

---

## Handoff Guide for Future Coding Agents

If you are continuing work on WirelessScanner:
- Review `docs/ARCHITECTURE.md` for design boundaries and provenance rules.
- Review `docs/DECISIONS.md` for historical ADRs.
- Check `docs/ROADMAP.md` for the current milestone target.
- For Phase 2 (BLE Observation):
  - Introduce `:core:scanner-api` to formalize the `RadioCollector` interface.
  - Create `:core:ble` to implement Android BLE scanning with runtime permission guards.
  - Connect the collector to `:app:ui:screens:observations`.
- Keep `:core:model` free of Android SDK dependencies.
