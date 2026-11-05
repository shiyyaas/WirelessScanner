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

# 2. Phase 3 — Wi-Fi Access Point Discovery

*Goal:* Implement passive Wi-Fi access point discovery using standard Android Wi-Fi APIs.

## Core implementation

Use:
•  WifiManager.
•  ScanResult.
•  BroadcastReceiver.
•  Kotlin Coroutines.
•  Flow.

Create a separate Wi-Fi scanner implementation.

Suggested architecture:

:core:model
    |
    |-- Observation
    |-- RadioType
    |-- SignalStrength
    |
:app
    |
    |-- wifi/
    |   |-- AndroidWifiScanner
    |   |-- WifiScanSessionManager
    |   |-- WifiPermissionManager
    |   |-- WifiResultMapper
    |   |-- WifiDeviceAggregator
    |
    |-- ui/
        |-- WifiScreen
        |-- WifiViewModel

Keep Wi-Fi scanning independent from BLE scanning.

## Information to collect

For each discovered access point:

•  SSID.
•  BSSID, when available.
•  RSSI.
•  Frequency.
•  Channel.
•  Frequency band.
•  Channel width, where supported.
•  Security capabilities.
•  First seen timestamp.
•  Last seen timestamp.
•  Observation count.
•  RSSI history.
•  Median RSSI.
•  Data freshness.

Handle:
•  Hidden SSIDs.
•  Missing BSSIDs.
•  Randomized MAC addresses.
•  Duplicate scan results.
•  Stale cached scan results.
•  Scan failures.
•  Wi-Fi disabled state.
•  Permission denial.

## Permissions

Handle Android versions correctly.

Include the required permissions for:
•  Wi-Fi state access.
•  Wi-Fi scan requests.
•  Location access where required.
•  Nearby Wi-Fi devices where applicable.

Important:
Android 10 devices generally require location permission and enabled location services to obtain Wi-Fi scan results.

Android 13+ Nearby Wi-Fi Devices permission does not replace location permission for every WifiManager scanning operation.

Do not request background location.

## Scan behavior

•  User-controlled scanning.
•  Respect Android Wi-Fi scan throttling.
•  Do not continuously poll the Wi-Fi scanner.
•  Listen for scan result broadcasts.
•  Check whether results are fresh or cached.
•  Display the last successful scan time.
•  Display scan failure information.
•  Do not promise that every scan request will produce fresh results.

## UI

Create a dedicated Wi-Fi screen.

Display:
•  Start/stop scan controls.
•  Wi-Fi state.
•  Permission status.
•  Last scan timestamp.
•  Number of discovered access points.
•  SSID.
•  BSSID.
•  RSSI.
•  Frequency.
•  Channel.
•  Band.
•  Security information.

Add:
•  Sorting by RSSI.
•  Sorting by frequency.
•  Sorting by SSID.
•  Filtering by band.
•  Filtering by security.
•  Expandable access point details.

## Channel analysis

Add basic channel analysis:
•  Access point count per channel.
•  Average RSSI per channel.
•  2.4 GHz channel distribution.
•  5 GHz channel distribution.
•  6 GHz channel distribution where supported.

Do not assume every channel is available in every country.

## Testing

Write tests for:
•  Wi-Fi result mapping.
•  Frequency-to-band conversion.
•  Frequency-to-channel conversion.
•  Security capability parsing.
•  Hidden SSIDs.
•  Missing BSSIDs.
•  RSSI aggregation.
•  Scan freshness.
•  Scan failures.
•  Scan throttling behavior.

## Completion criteria

•  Wi-Fi discovery works on supported devices.
•  Permission handling works across Android versions.
•  Stale and fresh results are distinguished.
•  Scanning respects platform restrictions.
•  UI displays discovered access points.
•  Unit tests pass.
•  Debug APK builds successfully.
•  Physical-device testing is completed.

---

# 3. Phase 4 — Unified Observation Engine

*Goal:* Bring BLE and Wi-Fi observations together without confusing their identities.

## Core architecture

Create a unified observation engine.

Suggested architecture:

                 Observation Engine
                         |
              ---------------------
              |                   |
          BLE Collector       Wi-Fi Collector
              |                   |
              ---------------------
                         |
                  Observation Store
                         |
                  Aggregation Engine
                         |
                    UI Layer

## Responsibilities

•  Receive observations from BLE.
•  Receive observations from Wi-Fi.
•  Preserve the original radio type.
•  Preserve receiver/source information.
•  Normalize timestamps.
•  Maintain observation history.
•  Aggregate repeated observations.
•  Calculate rolling median RSSI.
•  Track first seen and last seen.
•  Track observation count.
•  Maintain bounded RSSI history.

## Identity rules

Important:

•  Never assume a BLE address and Wi-Fi BSSID identify the same device.
•  Never assume two different addresses belong to different physical devices.
•  Do not use SSID alone as a device identity.
•  Account for MAC randomization.
•  Keep radio provenance.
•  Do not claim exact physical distance from RSSI.
•  Do not infer direction from RSSI alone.

## UI

Create a unified discovery screen.

Display:
•  All discovered observations.
•  BLE-only filter.
•  Wi-Fi-only filter.
•  Signal strength.
•  First seen.
•  Last seen.
•  Observation count.
•  Radio type.
•  Source information.

Add sorting and filtering across both radio types.

## Testing

Test:
•  Multi-radio observation handling.
•  Aggregation.
•  Duplicate handling.
•  Source preservation.
•  Identity isolation.
•  Timestamp normalization.
•  RSSI history.
•  Filtering and sorting.

## Completion criteria

BLE and Wi-Fi must work independently and together without cross-radio identity mistakes.

---

# 4. Phase 5 — Local Database and Observation History

*Goal:* Make WirelessScanner capable of retaining observations across app sessions.

## Technology

Use:
•  Room Database.
•  Kotlin Coroutines.
•  Flow.
•  Repository pattern.

## Database design

Suggested entities:

### ObservationEntity
•  id
•  identifier
•  radioType
•  displayName
•  rssi
•  frequency
•  channel
•  firstSeen
•  lastSeen
•  observationCount
•  source

### SignalSampleEntity
•  id
•  observationId
•  rssi
•  timestamp

### DeviceMetadataEntity
•  id
•  observationId
•  manufacturerData
•  serviceData
•  securityInformation
•  additionalMetadata

Store only information that is useful and appropriate.

## Features

•  Persist discovered observations.
•  Store RSSI history.
•  Display previously discovered devices.
•  Search historical observations.
•  Filter by date.
•  Filter by radio type.
•  View signal history.
•  Delete individual records.
•  Clear all history.
•  Configure retention limits.

## Export

Support:
•  CSV export.
•  JSON export.

Exports should preserve:
•  Timestamp.
•  Radio type.
•  Observation identifier.
•  RSSI.
•  Frequency.
•  Channel.
•  Source.
•  Available metadata.

## Completion criteria

•  Database migrations work.
•  History survives app restarts.
•  Database queries are efficient.
•  Exported data is valid.
•  Tests cover repositories and database operations.

---

# 5. Phase 6 — Device Signature and Classification Engine

*Goal:* Help users understand what kind of radio device they are observing.

This phase should use explainable rules rather than unsupported claims.

## Classification engine

Create a rule-based classification system.

Potential categories:
•  Smartphone.
•  Laptop.
•  Wireless headphones.
•  Smartwatch.
•  Smart television.
•  Router.
•  Access point.
•  IoT device.
•  Unknown BLE device.
•  Unknown Wi-Fi device.

## Classification inputs

Use available information:
•  BLE service UUIDs.
•  Manufacturer identifiers.
•  Advertisement types.
•  Advertisement metadata.
•  Wi-Fi security capabilities.
•  Frequency.
•  Vendor information where legitimately available.
•  Repeated observation patterns.

## Confidence

Each classification should contain:
•  Suggested category.
•  Confidence level.
•  Reasons for classification.
•  Supporting observations.
•  Limitations.

Example:

Device classification:
Possible wireless headphones

Confidence:
Medium

Reasons:
•  Observed BLE service information.
•  Advertisement pattern matches a known category.

Important:
Classification is an estimate, not proof of device identity.

## Signature database

Create a maintainable local signature database.

Allow:
•  Adding signatures.
•  Updating signatures.
•  Disabling outdated signatures.
•  Versioning signature data.

Document the licensing and attribution of any external datasets.

## Completion criteria

•  Classification rules are explainable.
•  Unknown devices remain unknown.
•  Confidence is not overstated.
•  Classification tests pass.

---

# 6. Phase 7 — Radar and Investigation Interface

*Goal:* Create a professional interface for understanding nearby radio observations.

## Main dashboard

Display:
•  Total observations.
•  BLE observation count.
•  Wi-Fi observation count.
•  Active scanning state.
•  Strongest signals.
•  Recently discovered observations.

## Radar interface

Create a visual discovery interface.

Possible elements:
•  Radar-style circular display.
•  Observation markers.
•  Radio-type indicators.
•  Signal strength indicators.
•  Observation detail panel.

Important:
The radar is a visualization of detected observations.

It must NOT claim to show:
•  Exact physical location.
•  Real-world direction.
•  Exact distance.
•  Actual movement.

RSSI is affected by obstacles, antennas, interference, device orientation and transmission power.

## Investigation mode

Allow users to:
•  Select an observation.
•  View its metadata.
•  Inspect RSSI history.
•  View first and last seen.
•  Compare signal samples.
•  Add investigation notes.
•  Mark observations as interesting.
•  Search previous observations.

## Completion criteria

•  UI remains responsive with many observations.
•  Radar visualization is clearly labeled.
•  No misleading positioning information.
•  Accessibility and dark theme are supported.

---

# 7. Phase 8 — Optional ESP32 CYD USB Receiver

*Goal:* Add optional external radio observation hardware.

This is an optional extension, not a requirement for the main Android application.

## Hardware

Potential hardware:
•  ESP32 CYD.
•  Compatible ESP32 board.
•  USB connection.
•  USB OTG-compatible Android phone.

## Communication

Use:
•  USB serial communication.
•  Newline-delimited JSON.
•  115200 baud initially, configurable if necessary.

Example message:

{
  "source": "esp32_cyd",
  "radio": "wifi",
  "timestamp": 1790000000,
  "frequency": 2437,
  "rssi": -62,
  "identifier": "example"
}

The actual protocol must be documented and versioned.

## Android integration

Create:
•  USB connection manager.
•  Serial communication layer.
•  JSON parser.
•  External observation mapper.
•  Receiver status screen.

Preserve:
•  Receiver identity.
•  Radio type.
•  Timestamp.
•  Hardware metadata.
•  Original observation source.

## Hardware responsibilities

The ESP32 may provide additional 2.4 GHz observation capabilities within its hardware and firmware limitations.

Do not assume:
•  Full-band simultaneous monitoring.
•  Unlimited packet capture.
•  Reliable device identification.
•  Exact physical positioning.

Keep this feature optional.

The Android BLE and Wi-Fi scanners must continue working without external hardware.

## Safety restrictions

Do not implement:
•  Deauthentication attacks.
•  Packet injection.
•  Credential cracking.
•  Unauthorized network access.
•  Disruption of nearby wireless devices.

## Completion criteria

•  USB connection is stable.
•  Disconnects are handled.
•  Invalid serial data is handled.
•  Observations preserve receiver provenance.
•  Android-only functionality remains unaffected.

---

# 8. Phase 9 — Cellular Information and Reporting

*Goal:* Add useful cellular network information using supported Android APIs.

## Information

Where Android and device APIs permit:
•  SIM state.
•  Network operator.
•  Network type.
•  Signal strength.
•  Cell information.
•  Mobile network status.

Handle:
•  Permission restrictions.
•  Android version differences.
•  Device manufacturer differences.
•  Missing information.
•  Unsupported fields.

Do not assume all cellular information is accessible on every device.

## Reporting

Create reports containing:
•  Observation summaries.
•  BLE results.
•  Wi-Fi results.
•  Cellular information.
•  Signal history.
•  Time range.
•  Source information.
•  Classification results.

## Export formats

•  CSV.
•  JSON.
•  PDF report.

## Completion criteria

•  Permissions are handled correctly.
•  Unsupported information is displayed honestly.
•  Reports preserve data provenance.
•  Export works offline.

---

# 9. Phase 10 — Optimization, Security and Production Readiness

*Goal:* Prepare WirelessScanner for reliable everyday use.

## Performance

•  Optimize scanning sessions.
•  Reduce unnecessary database operations.
•  Limit RSSI history size.
•  Avoid excessive UI recomposition.
•  Optimize large observation lists.
•  Manage memory usage.
•  Avoid unnecessary background work.

## Reliability

•  Handle Bluetooth state changes.
•  Handle Wi-Fi state changes.
•  Handle permission revocation.
•  Handle app lifecycle changes.
•  Handle USB disconnection.
•  Recover from scanner errors.
•  Prevent duplicate scan sessions.

## Security and privacy

•  Keep observations local by default.
•  No mandatory cloud account.
•  No unnecessary data collection.
•  No embedded credentials.
•  Secure release signing.
•  Review exported data.
•  Document permissions clearly.

## Testing

•  Unit tests.
•  Repository tests.
•  Database tests.
•  ViewModel tests.
•  Permission tests.
•  UI tests.
•  Physical-device tests.
•  Regression tests.

## CI/CD

Improve GitHub Actions to:
•  Run tests automatically.
•  Compile Debug APK.
•  Upload Debug APK artifacts.
•  Run release build checks.
•  Validate pull requests.
•  Preserve build reports.
•  Optionally create signed release builds using GitHub Secrets.

Never store signing keys or passwords directly in the repository.

---

# 10. Final Application Structure

Suggested final modular architecture:

WirelessScanner/
|
|-- app/
|   |-- navigation/
|   |-- ui/
|   |-- permissions/
|   |-- dashboard/
|   |-- investigation/
|   |-- radar/
|   |
|   |-- ble/
|   |-- wifi/
|   |-- cellular/
|   |-- usb/
|
|-- core/
|   |-- model/
|   |-- scanner-api/
|   |-- observation/
|   |-- aggregation/
|   |-- classification/
|   |-- common/
|
|-- data/
|   |-- database/
|   |-- repository/
|   |-- export/
|   |-- signatures/
|
|-- docs/
|   |-- ARCHITECTURE.md
|   |-- ROADMAP.md
|   |-- DECISIONS.md
|   |-- PRIVACY.md
|   |-- TESTING.md
|   |-- HARDWARE.md
|
|-- .github/
|   |-- workflows/
|       |-- ci.yml
|
|-- README.md
|-- AGENTS.md

This is a target architecture. Introduce modules only when they provide a clear architectural benefit. Do not create unnecessary modules just to match the diagram.

---

# 11. Development Rules for Every Future Phase

These rules apply to all future implementation work.

1. Inspect the existing repository before making changes.
2. Read AGENTS.md and the architecture documentation.
3. Do not rewrite working code unnecessarily.
4. Preserve existing module boundaries.
5. Keep the domain model platform-independent.
6. Keep BLE and Wi-Fi collectors independent.
7. Use interfaces for hardware-specific implementations.
8. Use Kotlin Coroutines and Flow appropriately.
9. Write unit tests for new functionality.
10. Update documentation.
11. Respect Android permission requirements.
12. Avoid unsupported Android APIs.
13. Avoid unnecessary dependencies.
14. Check dependency licenses.
15. Keep the app usable without internet access.
16. Do not introduce secrets into Git.
17. Run all relevant tests before completing a phase.
18. Build the Debug APK.
19. Provide exact test and build results.
20. Never merge a pull request without explicit approval.

## Jules implementation workflow

For every phase:

1. Give Jules the phase requirements.
2. Ask Jules to inspect the repository.
3. Ask Jules to present an implementation plan.
4. Review the plan.
5. Approve the plan before implementation.
6. Let Jules implement the changes.
7. Review the complete Git diff.
8. Review tests and build results.
9. Download and test the APK when applicable.
10. Review the pull request.
11. Merge only after approval.

Do not ask Jules to implement multiple major phases simultaneously.

---

# 12. Final Project Vision

WirelessScanner should become a modular Android wireless observation and investigation tool.

The final application should provide:

•  BLE discovery.
•  Wi-Fi access point discovery.
•  Unified observation management.
•  Local observation history.
•  RSSI history and analysis.
•  Explainable device classification.
•  Radar-style visualization.
•  Investigation tools.
•  CSV and JSON exports.
•  PDF reporting.
•  Optional ESP32 USB receiver integration.
•  Cellular information where supported.
•  Reliable offline operation.
•  Automated GitHub builds.
•  Comprehensive testing.
•  Production-ready Android UI.

## Important technical limitations

WirelessScanner is an observation and analysis application.

Standard Android APIs do not provide unrestricted raw Wi-Fi monitor mode.

RSSI cannot reliably establish exact distance or direction.

A MAC address does not guarantee permanent device identity.

Device classification must remain probabilistic.

External hardware capabilities depend on the hardware, firmware and regulatory environment.

All scanning must respect Android permissions, platform restrictions and applicable laws.

---

# 13. Recommended Execution Order

| Phase | Feature | Priority |
|---|---|---|
| 1 | Project foundation | Completed |
| 2 | BLE discovery | Implemented and merged |
| 3 | Wi-Fi discovery | Next |
| 4 | Unified observation engine | High |
| 5 | Local database and history | High |
| 6 | Device classification | High |
| 7 | Radar and investigation UI | High |
| 8 | ESP32 USB receiver | Optional |
| 9 | Cellular information and reporting | Later |
| 10 | Optimization and production readiness | Final |

*Development principle: Build a reliable foundation first. Add advanced features only after the underlying functionality is tested and stable.*
