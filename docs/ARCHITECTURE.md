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

## Module Structure

The project starts with a focused foundation avoiding premature multi-module overhead while strictly enforcing domain purity:

```
WirelessScanner/
├── core/
│   └── model/              # Pure Kotlin JVM module (no Android dependencies)
│       └── Domain models (RadioType, ReceiverSource, Observation, SignalStrength)
├── app/                    # Android Application module
│   ├── ui/                 # Jetpack Compose UI, Material 3 Theme, Navigation
│   │   ├── dashboard/      # Overview dashboard
│   │   ├── observations/   # Observation list and inspection
│   │   ├── receivers/      # Hardware & receiver status
│   │   ├── settings/       # App settings & preferences
│   │   └── navigation/     # NavHost and Destination graph
│   └── collector/          # Temporary collector contracts (future :core:scanner-api)
└── docs/                   # Architecture, Roadmap, and Decision records
```

### Planned Future Modules
- `:core:scanner-api`: Abstractions for pluggable radio collectors (`RadioCollector`, `ScanConfig`, `ScanStatus`).
- `:core:ble`: Android BLE Scanner implementation using `android.bluetooth.le`.
- `:core:wifi`: Android Wi-Fi Scanner implementation using `android.net.wifi.WifiManager`.
- `:core:receiver-usb`: USB Serial / OTG driver for optional ESP32 receivers.
- `:core:database`: Room persistence for observation history and sessions.
- `:core:classifier`: Evidence-based signature matching library.

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

- `rawPayload` is stored as an immutable `List<Byte>` (or defensively copied array) to prevent external mutation.
- `receiverSource` explicitly tags whether an observation was received by the Android device's internal radio or by an external hardware peripheral.
