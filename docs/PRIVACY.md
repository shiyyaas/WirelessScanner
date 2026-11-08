# WirelessScanner Privacy Architecture & Storage

WirelessScanner is designed with **Privacy by Default** as a core guiding constraint (`ROADMAP.md`). This document details what data is collected, where it is stored, retention policies, backup exclusions, and user controls.

---

## 1. Core Privacy Principles

1. **Local-Only Processing**: All observations, scan sessions, and aggregations are processed and stored entirely on-device within the application sandbox.
2. **Zero Network Upload**: WirelessScanner makes **no** network calls, telemetry submissions, crash reporting uploads, or analytics telemetry of any kind.
3. **Explicit Opt-In for Location**: Location permissions and services are strictly optional. Standard BLE and Wi-Fi observation operate locally without requiring geo-location tracking.
4. **Backup Exclusion**: Per Android manifest configuration, local application data is explicitly excluded from cloud backups.

---

## 2. Data Storage & Local Persistence

- **Database**: Local storage is managed via Room (`:core:database`) using standard SQLite databases private to the application sandbox (`/data/data/app.wirelessscanner/databases/`).
- **Data Types Stored**:
  - Scan session metadata (session ID, title, start/end timestamps, radio type).
  - Individual observation records (`Observation` domain model): timestamp, radio type (`BLE`, `WIFI`), receiver source (`Internal`, `ExternalUsbReceiver`), raw MAC address / BSSID, advertised name / SSID, signal strength (RSSI), raw payload bytes (`List<Byte>`), and metadata map.
- **Backup Exclusion (`AndroidManifest.xml`)**:
  - The application manifest (`app/src/main/AndroidManifest.xml`) configures `android:allowBackup="false"` on the `<application>` element. This prevents Android from backing up or restoring application data during cloud backup routines or device migrations.

---

## 3. Retention & Purge Controls

- **Manual Purge**: Users retain complete control over their stored data via in-app settings, allowing immediate deletion of individual scan sessions or complete database clearing.
- **Retention**: Data remains stored locally until explicitly purged by the user.

---

## 4. Export & User-Initiated Data Sharing

- **Export Formats**: Users can export scan sessions locally as JSON and CSV files via standard Android file intents (`DocumentsProvider` / SAF).
- **Control**: Export is strictly user-initiated. No data leaves the device unless the user explicitly shares the exported file.

---

## 5. Permissions & Platform Transparency

WirelessScanner requests only the minimum permissions required for passive radio observation:
- **BLE Discovery**: `BLUETOOTH_SCAN` (Android 12+) and `ACCESS_FINE_LOCATION` (Android 8–11). Note that `neverForLocation` is deliberately omitted per ADR 004 to prevent silent OS filtering of unassociated beacons.
- **Wi-Fi Discovery**: `ACCESS_FINE_LOCATION`, `ACCESS_WIFI_STATE`, `CHANGE_WIFI_STATE`, and `NEARBY_WIFI_DEVICES` (Android 13+).
- **Location Opt-In**: Used solely to satisfy Android system requirements for wireless scanning APIs; location coordinates are never logged, tracked, or associated with observation payloads.
