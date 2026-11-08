# WirelessScanner Project Roadmap

This roadmap defines the planned evolution of WirelessScanner. Each phase builds on the previous one while keeping the project modular, permissively licensed, privacy-preserving, and strict about observation provenance.

**Last reviewed:** 2026-11-08
**Status legend:** `[x]` done · `[ ]` planned · *Research spike* = time-boxed investigation, not a commitment

## How MVPs work in this roadmap

Every phase has an **MVP**: the smallest slice that delivers a user-visible outcome, can be verified on a real device, and respects the guiding constraints below. A phase is **MVP-complete** when its MVP checklist is done and the app is shippable. Everything under **Beyond MVP** is hardening and polish that can be scheduled later or cut without breaking the phase's purpose.

| Phase | MVP outcome |
|---|---|
| 1. Foundation | App builds, runs, navigates, and CI is green. *(Achieved)* |
| 2. BLE discovery | User starts a scan and sees live BLE devices. *(Achieved)* |
| 3. Wi-Fi discovery | User sees nearby access points with channel analysis. *(Achieved)* |
| 4. Persistence | Sessions are saved locally, exportable, and purgeable. *(Achieved)* |
| 4.5. Compliance baseline | Licence status of the codebase is known and enforced in CI. |
| 5. ESP32 USB receiver | Plug in one ESP32, see its observations tagged as external. |
| 6. Classification | Devices show explainable "possible" labels with evidence. |
| 7. Signal view | Per-device signal strength and trend, with no distance claims. |
| 8. Watch, mute & alerts | One watch rule raises one local notification during a session. |
| 9. Tracker awareness | Known tracker advertisement formats are flagged as possible trackers. |
| 10. Reports & survey | A session exports as JSON/CSV with full provenance. |

---

## Guiding Constraints (apply to every phase)

1. **Passive observation only.** No deauthentication, spoofing, injection, exploitation, or interception of protected traffic, in the app or in any reference receiver firmware.
2. **Provenance first.** Every observation records its receiver source, timestamp, raw identifier, and signal strength. Evidence is never synthesized.
3. **Hypotheses, not facts.** Classifications carry confidence and an evidence trail. OUI or company-ID attribution is never proof of an operator's identity or intent.
4. **No physical claims from RSSI.** RSSI alone is never presented as distance or direction. Matching addresses or names across radios never implies the same physical device (address randomization).
5. **Dependency policy.** Only permissively licensed dependencies (MIT, BSD, Apache-2.0). GPL/AGPL are excluded. LGPL requires an explicit ADR before use. Every adopted dependency or reused file is recorded with version, licence, and notices.
6. **Privacy by default.** On-device processing, no network upload, sensitive databases excluded from backup, location tagging strictly opt-in, no analytics by default.
7. **Platform honesty.** The app only reports what Android's supported APIs expose, and the UI says so when results are throttled, cached, or incomplete.

## Non-Goals

- Monitor-mode capture, packet injection, or decoding of arbitrary radio traffic.
- Wideband spectrum analysis or calibrated RF measurement on the phone's built-in radios.
- Bluetooth Classic discovery, Wi-Fi client/probe-only station tracking, or direction finding.
- Identifying or locating specific individuals.
- Indoor positioning or location inference from radio fingerprints.

---

## Phase 1: Project Foundation (Completed)

**MVP (achieved)**
- [x] Gradle Kotlin DSL scaffolding with version catalog (`libs.versions.toml`).
- [x] Toolchain: AGP 8.9.1, Gradle 8.11.1, Kotlin 2.2.0, JDK 21, compileSdk 36.
- [x] Pure domain module `:core:model` for platform-independent immutable observation records.
- [x] `:app` module with Jetpack Compose, Material 3 theming, and navigation graph.
- [x] Navigation scaffold: Dashboard, Observations, Receivers, Settings.
- [x] Android `.gitignore` and GitHub Actions CI workflow.

**Also delivered**
- [x] Unit test infrastructure for domain models and UI states.
- [x] Architecture documentation, ADRs, and roadmap.

## Phase 2: Bluetooth Low Energy (BLE) Discovery (Completed)

**MVP (achieved)**
- [x] `AndroidBleScanner` using `callbackFlow` and `BluetoothLeScanner`.
- [x] Pluggable `RadioCollector` lifecycle (`BleScanSessionManager`).
- [x] Android 8–11 (`ACCESS_FINE_LOCATION`) and Android 12+ (`BLUETOOTH_SCAN`) permission handling with transparent `neverForLocation` omission.
- [x] Bluetooth hardware state monitoring (`BluetoothStateManager`).
- [x] Advertisement parsing (`BleScanResultMapper`): names, UUIDs, manufacturer data, service data, Tx power, connectability.
- [x] BLE feed UI with live countdown and start/stop controls.

**Also delivered**
- [x] In-memory aggregation (`BleDeviceAggregator`): 30-sample history, rolling median RSSI, observation counts.
- [x] Search/filter and expandable device cards.
- [x] Unit tests with fake scanner implementations.

**Beyond MVP**
- [ ] Guard against scan-restart abuse. Android limits BLE scan starts (about 5 per 30 seconds since Android 7), and excess restarts can be silently downgraded to opportunistic scans.
- [ ] Decide how sessions longer than 15 seconds are handled (duty-cycled scan pulses vs. a single long scan) and record it in an ADR.

## Phase 3: Android Wi-Fi Observation & Deduplication Feed (Completed)

**MVP (achieved)**
- [x] Wi-Fi collector using `WifiManager` scan results.
- [x] Permission, scan-throttling, and location-service handling.
- [x] Normalization of SSID, BSSID, RSSI, frequency, and capabilities into `Observation`.
- [x] Wi-Fi discovery feed showing grouped access points.

**Also delivered**
- [x] Channel analysis UI with sorting and filtering.
- [x] Unit tests for mapping, conversion, aggregation, and view models.

**Beyond MVP**
- [ ] Surface throttling state in the UI. Android 9+ limits foreground apps to roughly 4 scans per 2 minutes and background apps to 1 per 30 minutes. A Developer Options toggle can disable throttling on Android 10+, but its availability varies by manufacturer.
- [ ] Show a per-result freshness indicator (fresh vs. cached) in the AP card, not only in banners.
- [ ] Add a plain-language note beside channel analysis that AP count and average RSSI do not prove congestion.

## Phase 4: Local Persistence with Room (Completed)

**MVP (achieved)**
- [x] `:core:database` for observation histories and scan sessions.
- [x] Sensitive databases excluded from cloud backup / auto-backup.
- [x] Purge functionality.

**Also delivered**
- [x] Session naming, search, and history export.

**Beyond MVP**
- [ ] Room schema export plus automated migration tests.
- [ ] Configurable retention (auto-purge after N days) with a conservative default.

---

## Phase 4.5: Compliance, Provenance & Documentation Baseline (Next)

Closes the gap between what the project claims and what has been verified. The research reference document lists code reuse as unaudited, so this phase comes before more features.

**MVP**
- [ ] **Provenance audit.** Review git history, `settings.gradle.kts`, module `build.gradle.kts` files, the version catalog, lockfiles, source headers, and any vendored code. Compare candidate files against the reference repositories and record the result in the repo.
- [ ] **CI licence gate.** Generate a dependency licence report on every build. Fail on GPL/AGPL, flag LGPL and unknown licences for manual review.
- [ ] **Notices.** Add `THIRD_PARTY_NOTICES` (or equivalent), a `NOTICE` file where required, and decide the project's own licence.
- [ ] **Docs sync.** Update `ARCHITECTURE.md` to match the code. Known drift: it omits `:core:database`; module paths differ from the roadmap (`:app:ui:screens:observations` vs. `app/ui/observations`); source naming varies (`Internal`, `InternalRadio`, `ExternalUsbReceiver`); the receiver hardware is named as both ESP32 CYD and ESP32-S3; and cellular appears in the overview but has no roadmap phase.

**Beyond MVP**
- [ ] **Feature provenance matrix.** Mark each feature as independently implemented, adapted (with upstream commit and licence), dependency-backed, or not implemented.
- [ ] **SBOM.** Produce a CycloneDX or SPDX bill of materials per release build.
- [ ] **ADRs.** Record the dependency policy, `Observation` schema versioning, and the BLE long-session strategy.
- [ ] **Limitations page.** Publish a user-facing "What this app can and cannot see" document, covering no monitor mode, no probe-only clients, no Bluetooth Classic, no direction finding, address randomization, and throttling.
- [ ] **Device test matrix.** Define the minimum set of physical devices and Android versions (8 through the latest) used for manual verification before each release.

---

## Phase 5: ESP32 USB Receiver Infrastructure

**MVP:** with one ESP32-S3 plugged in over USB OTG, the user grants permission and sees its observations in the feed, tagged as external. Unplugging it does not crash the app.
- [ ] **ADR: USB serial approach.** Choose between Android's USB Host API directly and a library. Check the licence of any library at the exact version before adopting it. Some `usb-serial-for-android` READMEs and forks describe older releases as LGPL-2.1, so confirm the current licence against the dependency policy.
- [ ] **Minimal protocol spec** in `docs/`: newline-delimited JSON with `schemaVersion`, `receiverId`, `firmwareVersion`, and the observation fields. Unknown fields are ignored.
- [ ] **`:core:receiver-usb` module** for USB CDC serial communication, isolated from Android UI.
- [ ] **Parser** that counts and skips malformed lines instead of crashing, with unit tests against golden fixture files.
- [ ] **Lifecycle basics.** USB permission flow and attach/detach handling.
- [ ] **Provenance.** Tag observations as `ReceiverSource.ExternalUsbReceiver` with the `receiverId`. Never merge external and internal observations by address.
- [ ] **Receivers screen (minimal).** Connected or disconnected state and last frame time.

**Beyond MVP**
- [ ] Monotonic `seq`, length limits, and a documented clock-offset strategy between receiver and phone timestamps.
- [ ] Fuzz or property-based parser tests.
- [ ] Automatic reconnect, backpressure and rate limiting, heartbeat and stale-receiver detection, dropped-frame counters.
- [ ] Simulated receiver feed for development, CI, and demo mode without hardware.
- [ ] Receivers screen details: firmware version, throughput, parse-error counts.
- [ ] Reference firmware or protocol conformance suite in a separate module or repository. Passive reception only, with its own licence review.

---

## Phase 6: Signature Classification & Registries

**MVP:** devices in the feed show explainable "possible" labels. Tapping a label shows exactly which fields triggered it.
- [ ] **Bundled offline registries (starter set).** IEEE OUI lookup with longest-prefix matching across MA-L (24-bit), MA-M (28-bit), and MA-S (36-bit), plus Bluetooth SIG company identifiers. Confirm redistribution terms first: the IEEE listing's terms were not clearly found, and the SIG's Assigned Numbers document states it grants no licence to SIG or member IP.
- [ ] **Registry build script** that generates the data from its source and stamps a version and date, shown in the UI.
- [ ] **Randomized-address handling.** Detect locally administered addresses, skip OUI attribution for them, and label them as randomized.
- [ ] **Classification model:** `Classification(label, confidence, evidence[], ruleId, registryVersion)`.
- [ ] **Starter rule set** of 5–10 well-understood, low-risk rules, with wording like "possible" and "consistent with", never "is".
- [ ] **Evidence trail** shown on the device card.
- [ ] **Ethics review gate.** No rule for a sensitive category (cameras, public-safety beacons, trackers) ships without a wording and false-positive review.

**Beyond MVP**
- [ ] Declarative, versioned signature packs (rule id, description, matched fields, confidence, known false positives).
- [ ] User controls: dismiss a classification, add a local note, record a false positive locally.
- [ ] Regression test corpus of real-world advertisement fixtures, including known look-alikes.
- [ ] Bluetooth SIG 16-bit service UUID names, and community mirrors (for example Nordic's Bluetooth Numbers Database) after a licence check.
- [ ] Do not import OUI lists extracted from GPL-licensed tools unless their licence is verified compatible.

---

## Phase 7: Signal-Strength Visualization

**MVP:** each device shows its current signal strength, a short history, and whether it is getting stronger or weaker, with a note that this is not distance.
- [ ] Per-device current RSSI, rolling median, and 30-sample sparkline.
- [ ] "Getting stronger / weaker" trend indicator in place of distance estimates.
- [ ] One-tap explanation of what signal strength does and does not mean.
- [ ] TalkBack descriptions and non-colour-only encoding.

**Beyond MVP**
- [ ] Radar-style view. If kept, rings represent relative signal-strength bands, not distance, with no bearing. Prefer a layout that cannot be mistaken for a location map.
- [ ] Variance band to show measurement uncertainty.
- [ ] Reduced-motion setting.
- [ ] Performance budget: stay smooth with several hundred simultaneous devices.

---

## Phase 8: Watch, Mute & Alerts

**MVP:** the user creates one watch rule and gets one local notification when it matches during an active scan session. No background service.
- [ ] One watch rule type on stable attributes (service UUID, manufacturer ID, or name pattern), with a note that randomized addresses cannot be tracked.
- [ ] Mute for a device or rule.
- [ ] A single notification channel with a simple per-rule cooldown.
- [ ] Rule hits stored with the session, covered by existing purge and retention controls.

**Beyond MVP**
- [ ] Additional rule types (OUI, combined conditions) and mute expiry.
- [ ] Quiet hours and richer rate limiting.
- [ ] ADR on background operation: foreground-service requirements and types on current Android versions, battery impact, and the user-visible ongoing notification.
- [ ] Per-rule hit history view.

---

## Phase 9: Tracker Awareness (Defensive)

**MVP:** a small set of publicly documented tracker advertisement formats is recognized and shown as "possible tracker", with evidence and a link to safety guidance.
- [ ] Recognize publicly documented tracker advertisement formats only, as hypotheses with an evidence trail.
- [ ] Complement, not replace, the operating system's built-in unknown-tracker alerts, and say so in the UI.
- [ ] In-app guidance on what to do if the user believes they are being tracked, using official resources.
- [ ] No location history in the MVP.

**Beyond MVP**
- [ ] "Seen repeatedly across time" analysis, using location context only if the user opts in. Explain the limits: rotating addresses and passive-scan gaps cause missed or false detections.
- [ ] Align terminology and behaviour with the IETF DULT work. The accessory-protocol draft is still an Internet-Draft, so track it rather than treating it as final.

---

## Phase 10: Reports, Export & Survey Mode

**MVP:** a saved session exports as JSON and CSV including provenance fields and the versions that produced it.
- [ ] Export fields: receiver source, timestamps, raw identifiers, signal strength, scan-freshness flags, app version, registry and signature-pack versions, device model, and Android version.
- [ ] Export is user-initiated, local-only, and documented in the Limitations page.

**Beyond MVP**
- [ ] Readable HTML or PDF summary report.
- [ ] Optional redaction (truncate addresses, strip names) and an export integrity hash.
- [ ] Import of ESP32 session recordings for replay and analysis.
- [ ] Wi-Fi site-survey mode: user-placed waypoints or stop-and-go measurements, opt-in location, local-only storage, export/import.

---

## Continuous Tracks

**Platform compatibility**
- [ ] Review each new Android release's behaviour changes (Wi-Fi, Bluetooth, background limits, permissions) and raise `targetSdk` deliberately.
- [ ] Maintain the device matrix from Phase 4.5 and test on physical hardware across supported Android versions.
- [ ] Note OEM variation: the throttling toggle, scan limits, and Bluetooth stack behaviour differ by manufacturer.

**Privacy & security**
- [ ] Threat model document, including what the app's data would reveal if a device is lost or seized.
- [ ] Permission rationale screens explaining each permission in plain language.
- [ ] R8 minification, signed releases, dependency update automation with licence re-check.

**Quality**
- [ ] Fixture replay tests, ViewModel tests, screenshot tests for key screens.
- [ ] Battery and memory profiling for long sessions.
- [ ] Accessibility audit and localization readiness.

---

## Research Spikes (not committed)

| Topic | Question to answer | Constraint |
|---|---|---|
| Android Ranging module (Wi-Fi RTT / 802.11az, Bluetooth channel sounding, UWB; added in Android 16) | Can the app offer a distinct "measured range to a cooperating device" feature? | Generally requires cooperating hardware, so it does not apply to passive observation of arbitrary devices. Would be a separate measurement type, never mixed with RSSI. |
| Cellular observation | Commit to it, or remove it from the architecture overview? | Limited by Android telephony APIs and permissions. |
| Radio fingerprinting / indoor positioning | Not pursued. | Out of scope per non-goals. |
| External SDR hardware (RTL-SDR, HackRF) | Separate product capability? | Needs its own architecture, GPL-licensing analysis, and legal/safety review. Not part of phone-native scanning. |

---

## Definition of Done

**MVP gate (every phase)**
- [ ] Every item in the phase's MVP checklist is done.
- [ ] Unit tests for new logic, with fakes for hardware boundaries.
- [ ] Manual verification on at least two physical devices on different Android versions.
- [ ] CI green, including the licence gate (once Phase 4.5 lands).
- [ ] No distance or direction claims, and no unqualified identity claims, anywhere in the new UI or exports.

**Phase-complete gate (after MVP)**
- [ ] Beyond-MVP items are done or explicitly deferred with a note in this file.
- [ ] `ARCHITECTURE.md`, this roadmap, and any ADRs are updated in the same change.
- [ ] New dependencies are recorded in the notices file with version and licence.
