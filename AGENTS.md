WirelessScanner — Agent Instructions

Project goal

Build a modular Android application for observing nearby wireless signals using Android-supported APIs, with optional external receiver hardware in future versions.

The app is intended for lawful, passive observation and analysis. Do not implement intrusion, exploitation, credential collection, unauthorized access, or interference with wireless devices.

Technology

•  Kotlin
•  Android SDK
•  Jetpack Compose
•  Material 3
•  Coroutines and Flow
•  Room for local persistence when needed
•  Gradle Kotlin DSL
•  Version catalog for dependency management
•  JUnit for unit tests

Use stable, mutually compatible dependency versions. Verify versions and Android requirements rather than guessing.

Architecture principles

1. Keep radio-specific collectors separate.
2. Normalize collected data into a shared observation model.
3. Keep scanning, storage, classification, and UI responsibilities separate.
4. Make external sensors optional. The app must work without them.
5. Prefer small, testable modules and clear interfaces.
6. Avoid unnecessary dependencies and premature complexity.

Planned capabilities

•  Android BLE advertisement observation.
•  Android Wi-Fi access-point observation.
•  Unified observation history.
•  Device records and observation timelines.
•  Explainable signature-based classification in a later milestone.
•  Radar-style interface in a later milestone.
•  Optional ESP32 USB receiver integration in a later milestone.
•  Cellular information only as a later, separate capability.

Do not implement all these capabilities at once. Follow the milestones requested in each task.

Observation and data integrity

•  Preserve the original source and timestamp of every observation.
•  Distinguish an advertised Bluetooth name from a cached system device name.
•  Do not assume that matching names or addresses prove two observations came from the same physical device.
•  Treat device classifications as hypotheses, not facts.
•  Preserve the evidence behind every classification.
•  Do not invent distance, direction, or location from RSSI alone.
•  Handle stale or cached scan results explicitly.

Android platform requirements

•  Respect Android runtime permissions and version-specific restrictions.
•  Handle Bluetooth and Wi-Fi being disabled.
•  Handle scan failures, throttling, cancellation, and unavailable hardware.
•  Avoid unnecessary continuous scanning and battery consumption.
•  Use foreground services only when justified and compliant with Android requirements.
•  Never claim that a scan is successful unless the platform reports usable results.

Licensing

Reference projects may be studied for architecture and ideas.

Do not copy entire applications or introduce GPL-licensed source code into this project without explicit approval and license review.

Preserve copyright notices and required license files for any reused code or data.

Review dependencies and bundled datasets separately.

Development rules

•  Before making substantial changes, explain the implementation plan.
•  Work on one milestone at a time.
•  Do not silently change the architecture.
•  Add tests for important business logic.
•  Run relevant tests and builds after changes.
•  Report the exact commands executed and whether they succeeded.
•  Never claim a build or test passed if it was not actually run.
•  Do not hide warnings or failures.
•  Keep changes focused and reviewable.
•  Do not add secrets, credentials, signing keys, or personal data to the repository.

First milestone

Create the initial Android project foundation only:

•  Project and Gradle configuration.
•  Kotlin and Jetpack Compose setup.
•  Basic app entry point.
•  Simple Material 3 navigation structure.
•  Initial module structure, only where justified.
•  Unit-test infrastructure.
•  README with setup and build instructions.

Do not implement Wi-Fi scanning, BLE scanning, cellular scanning, USB communication, or device classification in the first milestone.
