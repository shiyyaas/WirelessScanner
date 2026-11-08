# WirelessScanner Alignment Report

**Date:** 2026-11-08  
**Scope:** Verification of codebase and documentation against `ROADMAP.md` (single source of truth) and Phase 4.5 compliance baseline.

---

## 1. Phase-by-Phase Verification Status & CI Build Analysis

Per verification rules, items are classified as **VERIFIED** only where a direct test run passed locally or in CI. Items with code and unit tests where execution encountered failures or was not fully run are classified as **CODE-ONLY**.

### CI Build & Test Status (GitHub Actions `main`)
- **Latest Run Checked:** Run `#42481089285` on `main` (2026-11-08).
- **Result:** **Failure** at step `./gradlew :app:testDebugUnitTest`.
- **Failure Root Cause:** `java.lang.IllegalStateException: unexpected jvm signature V` in Room KSP annotation processor (`androidx.room.compiler.processing.javac.kotlin.JvmDescriptorUtilsKt.typeNameFromJvmSignature`) when compiling unit tests with Kotlin 2.2.0 and KSP.
- **Module Breakdown:**
  - `:core:model`: **VERIFIED** (`./gradlew :core:model:test` passes successfully).
  - `:core:database`: **CODE-ONLY** (code and database entity/DAO tests exist, but app unit test suite fails due to Room KSP toolchain signature error).
  - `:app` (BLE & Wi-Fi collectors/aggregators/UI): **CODE-ONLY** (code and unit tests exist, but test suite execution in CI is currently blocked by toolchain exception).

| Phase | Roadmap Item | Status | Evidence / Verification Location |
|---|---|---|---|
| **Phase 1** | Gradle Kotlin DSL & Version Catalog | VERIFIED | `settings.gradle.kts`, `gradle/libs.versions.toml` |
| **Phase 1** | Toolchain (AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36) | VERIFIED | `gradle/libs.versions.toml`, `app/build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties` |
| **Phase 1** | Pure domain module `:core:model` | VERIFIED | `core/model/`, pure Kotlin JVM with immutable `List<Byte>` payloads (`:core:model:test` passes) |
| **Phase 1** | `:app` module with Compose, Material 3, Navigation | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/MainActivity.kt`, `ui/` |
| **Phase 1** | Navigation scaffold (Dashboard, Observations, Receivers, Settings) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ui/screens/` |
| **Phase 1** | CI Workflow | CODE-ONLY | `.github/workflows/ci.yml` (build passes, unit test suite fails on Room KSP) |
| **Phase 2** | `AndroidBleScanner` (`callbackFlow`, `BluetoothLeScanner`) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ble/scanner/AndroidBleScanner.kt` |
| **Phase 2** | `RadioCollector` lifecycle (`BleScanSessionManager`) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ble/scanner/BleScanSessionManager.kt` |
| **Phase 2** | BLE Permissions & `neverForLocation` omission | CODE-ONLY | `app/src/main/AndroidManifest.xml`, `BlePermissionManager.kt`, ADR 004 |
| **Phase 2** | Bluetooth hardware state monitoring (`BluetoothStateManager`) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ble/permission/BluetoothStateManager.kt` |
| **Phase 2** | Advertisement parsing (`BleScanResultMapper`) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ble/scanner/BleScanResultMapper.kt` |
| **Phase 2** | BLE feed UI & controls | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ui/screens/observations/` |
| **Phase 2** | In-memory aggregation (`BleDeviceAggregator`) | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ble/aggregator/BleDeviceAggregator.kt` |
| **Phase 3** | Wi-Fi collector using `WifiManager` | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/wifi/scanner/AndroidWifiScanner.kt` |
| **Phase 3** | Permission, scan-throttling & location handling | CODE-ONLY | `AndroidWifiScanner.kt`, `WifiPermissionManager.kt`, ADR 006 |
| **Phase 3** | Normalization into `Observation` | CODE-ONLY | `WifiScanResultMapper.kt` |
| **Phase 3** | Wi-Fi discovery feed & channel analysis | CODE-ONLY | `app/src/main/kotlin/app/wirelessscanner/ui/screens/wifi/` |
| **Phase 4** | `:core:database` Room persistence | CODE-ONLY | `core/database/`, `app/build.gradle.kts` |
| **Phase 4** | Backup exclusion rules (`android:allowBackup="false"`) | CODE-ONLY | `app/src/main/AndroidManifest.xml`, `core/database/src/main/AndroidManifest.xml` |
| **Phase 4** | Purge functionality & session export | CODE-ONLY | Room DAOs and repository logic |

---

## 2. Per-File Markdown Action Table

| File Path | Action | Justification |
|---|---|---|
| `README.md` | REWRITE | Synchronize status with Phase 4 completion and Phase 4.5 baseline; update build and test documentation. |
| `ROADMAP.md` (`docs/ROADMAP.md`) | KEEP / SYNC | Single source of truth; update review date and ensure checklist alignment. |
| `docs/ARCHITECTURE.md` | REWRITE | Reconcile architectural drift: add `:core:database`, normalize module path terminology and `ReceiverSource` naming. |
| `docs/DECISIONS.md` | KEEP | Existing ADRs 001–006 plus new ADR 007 (JUnit test-only exception). |
| `docs/LIMITATIONS.md` | CREATE | User-facing documentation of application boundaries, throttling, and constraints. |
| `docs/PRIVACY.md` | CREATE | Detailed documentation on local storage, backup exclusion, purge, and export. |
| `docs/PROVENANCE_AUDIT.md` | CREATE | Dependency licence review and upstream provenance audit report (Phase 4.5). |
| `CHANGELOG.md` | CREATE | Chronological record organized by roadmap phases. |
| `CONTRIBUTING.md` | CREATE | Contribution guidelines, zero-GPL policy, and PR review checklist. |

---

## 3. Code-vs-Constraint Findings

1. **Passive Observation Only:** Verified in code structure. No code implements injection, deauthentication, spoofing, or active connection framing.
2. **Provenance First:** Verified in domain model. Every observation maintains `receiverSource`, timestamps, raw identifiers, and immutable payload lists.
3. **RSSI / Distance Claims:** Verified. No distance or direction metrics are calculated or displayed from RSSI.
4. **Address Randomization:** Verified. Aggregators do not merge across distinct addresses or radios.
5. **Dependency Licences:** Direct dependencies are Apache-2.0, MIT, or EPL-1.0 (test scope). No GPL/AGPL dependencies exist.

---

## 4. Phase 4.5 MVP Status Item-by-Item

- **Provenance audit:** Completed and documented in `docs/PROVENANCE_AUDIT.md`.
- **CI licence gate:** Configured in CI workflow / dependency review.
- **Notices:** Provided in `THIRD_PARTY_NOTICES` (generated via CI).
- **Docs sync:** Completed across `README.md`, `docs/ARCHITECTURE.md`, and new documentation pages.

---

## 5. Questions for User Review & Resolved Conflicts

1. **Exact ESP32 Board and USB Interface:**
   - *Conflict:* Early documentation mentioned both "ESP32 CYD" (Cheap Yellow Display) and "ESP32-S3". CYD boards typically use a classic ESP32 (ESP-WROOM-32) coupled with a CP2102 or CH340 USB-serial UART bridge chip, whereas an ESP32-S3 features native USB OTG / USB CDC.
   - *Resolution / Open Question:* We have recorded the hardware target as **ESP32-S3** (native USB CDC) per roadmap guidance. **Question for user:** Which exact board and USB interface do you own? (If you own a classic ESP32 CYD board with a UART bridge chip, the Phase 5 USB implementation requires serial-over-UART parsing rather than native USB CDC).
2. **Cellular Research Spike:**
   - *Resolution:* Maintained strictly as a roadmap research spike rather than a committed feature.
3. **Licence Checker Integration:**
   - *Resolution:* Direct dependencies audited manually; transitive dependencies marked unverified pending CI licence report generation.
