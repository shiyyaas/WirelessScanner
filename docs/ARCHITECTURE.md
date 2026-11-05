# WirelessScanner Architecture

## Overview

WirelessScanner is an Android application designed for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, cellular signals and optional external USB hardware receivers such as ESP32 CYD).

This document outlines the architectural principles, component structure, and design patterns guiding development.

---

## Architectural Principles

1. **Passive Observation Only**: The application strictly observes signals broadcast over the air or captured by attached receiver hardware. It does not perform active intrusion, exploitation, spoofing, deauthentication, or interception of protected communications.
2. **Provenance & Data Integrity**:
   - Every observation records its original `receiverSource` (e.g. `InternalRadio` vs `ExternalUsbReceiver`), precise timestamp, raw identifier, and observed signal strength.
   - Observational evidence is never synthesized or fabricated. RSSI alone must never be used to claim physical distance or direction without verified multi-sensor fusion.
   - Physical identity across radios is never presumed: matching MAC addresses, BSSIDs, or advertised names do **not** imply that two observations originate from the same physical device due to address randomization and common naming schemes.
   - Classifications are treated as probabilistic hypotheses with supporting evidence, not absolute facts.
3. **Hardware Agnosticism & Optional Peripherals**:
   - Internal Android device radios (Bluetooth Adapter, Wi-Fi Manager) and optional external receivers (USB CDC/UART connected ESP32 microcontrollers) are modeled as distinct observation sources.
   - External hardware is completely optional; the application functions standalone on any compatible Android device.
4. **Modularity & Separation of Concerns**:
   - Radio-specific protocol decoding is isolated from core models.
   - Core domain models are free from Android SDK dependencies to guarantee testability and portability.
   - Unidirectional Data Flow (UDF) is maintained between the presentation, domain, and data/collector layers.

---

## Module Structure & BLE Component Architecture

```
WirelessScanner/
├── core/
│   └── model/              # Pure Kotlin JVM module (no Android dependencies)
│       └── Domain models (RadioType, ReceiverSource, Observation, SignalStrength)
├── app/                    # Android Application module
│   ├── ble/                # Bluetooth Low Energy Discovery
│   │   ├── aggregator/     # BleDeviceAggregator (rolling median, count, history)
│   │   ├── model/          # DiscoveredBleDevice domain model & BleAddressType
│   │   ├── permission/     # BlePermissionManager, BluetoothStateManager
│   │   └── scanner/        # AndroidBleScanner, BleScanSessionManager, BleScanResultMapper
│   ├── collector/          # RadioCollector abstraction & CollectorStatus
│   └── ui/                 # Jetpack Compose UI, Material 3 Theme, Navigation
│       ├── dashboard/      # Overview dashboard
│       ├── observations/   # Observation feed, controls & ViewModel
│       ├── receivers/      # Hardware & receiver status
│       ├── settings/       # App settings & preferences
│       └── navigation/     # NavHost and Destination graph
└── docs/                   # Architecture, Roadmap, and Decision records
```

### Bluetooth Low Energy Observation Flow
1. **Hardware Scanner Abstraction**: `BleHardwareScanner` decouples the low-level Android `BluetoothLeScanner` from session logic. `AndroidBleScanner` uses `callbackFlow` to safely stream scan results and cleanly release scanner callbacks upon completion or cancellation.
2. **Result Parsing & Immutability**: `BleScanResultMapper` safely maps `ScanResult` and `ScanRecord` to `Observation` (`RadioType.BLE`, `ReceiverSource.Internal`), ensuring byte arrays are immutably preserved as `List<Byte>`.
3. **Session Management**: `BleScanSessionManager` orchestrates user-controlled 15-second discovery sessions, preventing concurrent duplicate scans, exposing live countdown metrics, and automatically halting on timeout.
4. **In-Memory Aggregation**: `BleDeviceAggregator` groups observations by unique `rawAddress`, preserving first/last seen timestamps, count, latest RSSI, a 30-sample rolling RSSI history, rolling median calculation, and merging late-arriving advertised names.
5. **Presentation Layer**: `ObservationsViewModel` and `ObservationsScreen` expose user controls (Start/Stop, filter by name/query, sort by signal/time/count), hardware banners (Bluetooth disabled, permission required), and expandable advertisement details.

---

## Provenance Model

The core `Observation` model enforces end-to-end provenance:

```kotlin
data class Observation(
    val id: String,
    val timestampEpochMillis: Long,
    val radioType: RadioType,
    val receiverSource: ReceiverSource,
    val rawAddress: String,
    val advertisedName: String?,
    val signalStrength: SignalStrength,
    val rawPayload: List<Byte> = emptyList(), // Immutable byte representation
    val metadata: Map<String, String> = emptyMap()
)
```

- `rawPayload` is stored as an immutable `List<Byte>` to prevent external mutation.
- `receiverSource` explicitly tags whether an observation was received by the Android device's internal radio or by an external hardware peripheral.
