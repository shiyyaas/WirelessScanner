# WirelessScanner Architecture

## Overview

WirelessScanner is an Android application designed for lawful, passive observation of nearby wireless signals (BLE, Wi-Fi, and in future milestones, cellular signals and optional external USB hardware receivers such as ESP32-S3).

This document outlines the architectural principles, component structure, and design patterns guiding development.

---

## Architectural Principles

1. **Passive Observation Only**: The application strictly observes signals broadcast over the air or captured by attached receiver hardware. It does not perform active intrusion, exploitation, spoofing, deauthentication, or interception of protected communications.
2. **Provenance & Data Integrity**:
   - Every observation records its original `receiverSource` (`Internal` vs `ExternalUsbReceiver`), precise timestamp, raw identifier, and observed signal strength.
   - Observational evidence is never synthesized or fabricated. RSSI alone must never be used to claim physical distance or direction without verified multi-sensor fusion.
   - Physical identity across radios is never presumed: matching MAC addresses, BSSIDs, or advertised names do **not** imply that two observations originate from the same physical device due to address randomization.
   - Classifications are treated as probabilistic hypotheses with supporting evidence, not absolute facts.
3. **Hardware Agnosticism & Optional Peripherals**:
   - Internal Android device radios and optional external receivers (USB CDC connected ESP32-S3 microcontrollers) are modeled as distinct observation sources.
4. **Modularity & Separation of Concerns**:
   - Radio-specific protocol decoding is isolated from core models.
   - Core domain models (`:core:model`) are free from Android SDK dependencies to guarantee testability and portability.
   - Persistence is isolated in `:core:database`.

---

## Module Structure & Component Architecture

```
WirelessScanner/
├── core/
│   ├── model/              # Pure Kotlin JVM module (no Android dependencies)
│   │       └── Domain models (RadioType, ReceiverSource, Observation, SignalStrength)
│   └── database/           # Room persistence module (scan sessions & observations)
├── app/                    # Android Application module
│   ├── ble/                # Bluetooth Low Energy Discovery
│   │   ├── aggregator/     # BleDeviceAggregator (rolling median, count, history)
│   │   ├── model/          # DiscoveredBleDevice domain model
│   │   ├── permission/     # BlePermissionManager, BluetoothStateManager
│   │   └── scanner/        # AndroidBleScanner, BleScanSessionManager, BleScanResultMapper
│   ├── wifi/               # Wi-Fi Access Point Discovery
│   │   ├── aggregator/     # WifiDeviceAggregator (BSSID grouping, median, 30-sample window)
│   │   ├── model/          # DiscoveredWifiAccessPoint domain model, WifiBand, WifiSecurityType
│   │   ├── permission/     # WifiPermissionManager
│   │   ├── scanner/        # AndroidWifiScanner, WifiScanSessionManager, WifiScanResultMapper
│   │   └── util/           # WifiFrequencyConverter & security parsing
│   ├── collector/          # RadioCollector abstraction & CollectorStatus
│   └── ui/                 # Jetpack Compose UI, Material 3 Theme, Navigation
│       ├── dashboard/      # Overview dashboard
│       ├── observations/   # BLE observation feed, controls & ViewModel
│       ├── wifi/           # Wi-Fi discovery feed, channel analysis & ViewModel
│       ├── receivers/      # Hardware & receiver status
│       ├── settings/       # App settings & preferences
│       └── navigation/     # NavHost and Destination graph
└── docs/                   # Architecture, Roadmap, and Decision records
```

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
- `receiverSource` explicitly tags whether an observation was received by the Android device's internal radio (`Internal`) or by an external hardware peripheral (`ExternalUsbReceiver`).

---

## Documentation Links

- [Development Roadmap](ROADMAP.md)
- [Architecture Decision Records](DECISIONS.md)
- [Alignment Report](ALIGNMENT_REPORT.md)
- [Provenance Audit](PROVENANCE_AUDIT.md)
