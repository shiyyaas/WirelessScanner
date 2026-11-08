# Changelog

## Phase 4.5: Compliance & Provenance Baseline
- **Date: 2026-11-08**
- Established compliance baseline including provenance audit, CI license gating, architecture documentation sync, and limitation/privacy documentation.

## Phase 4: Local Persistence with Room
- **Date: 2026-11-08**
- Implemented `:core:database` module with Room persistence for observation histories and scan sessions.
- Added backup exclusion rules (`android:allowBackup="false"`).
- Added purge functionality.

## Phase 3: Android Wi-Fi Observation
- **Date: 2026-11-06**
- Implemented Wi-Fi collector using `WifiManager`.
- Added channel analysis UI and normalization of Wi-Fi access point observations.

## Phase 2: Bluetooth Low Energy (BLE) Discovery
- **Date: 2026-11-05**
- Implemented BLE scanner, scan session management, and in-memory aggregation.

## Phase 1: Project Foundation
- **Date: 2026-11-05**
- Established project foundation with Gradle Kotlin DSL, version catalog, CI workflow, and domain module.
