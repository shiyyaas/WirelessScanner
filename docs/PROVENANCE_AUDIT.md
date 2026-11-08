# WirelessScanner Provenance & Dependency Audit

**Date:** 2026-11-08  
**Scope:** Phase 4.5 Compliance Baseline — Direct dependencies, licence verification, and upstream provenance review.

---

## 1. Upstream Provenance Review

- **Status:** No automated code comparison against reference projects (`Fieldwatch`, `BLE-Device-Radar`, etc.) was performed; **human review required**.
- **Architecture Inspiration:** Architectural patterns (such as separation of pure domain models in `:core:model` and explicit `ReceiverSource` provenance tagging) were studied from permissively licensed reference projects (MIT / Apache-2.0), but no code was copied. All implementation logic in WirelessScanner is independently authored.

---

## 2. Direct Dependency Inventory & Licence Audit

*Note: Audit scope covers direct dependencies declared in `gradle/libs.versions.toml`. Transitive dependencies remain unverified pending automated SBOM generation.*

| Dependency | Group / Artifact | Version | Declared Licence | Scope | Notes / Compliance Status |
|---|---|---|---|---|---|
| AndroidX Core KTX | `androidx.core:core-ktx` | 1.15.0 | Apache-2.0 | Production | Permissive |
| AndroidX Lifecycle Runtime | `androidx.lifecycle:lifecycle-runtime-ktx` | 2.8.7 | Apache-2.0 | Production | Permissive |
| AndroidX Lifecycle Compose | `androidx.lifecycle:lifecycle-runtime-compose` | 2.8.7 | Apache-2.0 | Production | Permissive |
| AndroidX ViewModel Compose | `androidx.lifecycle:lifecycle-viewmodel-compose` | 2.8.7 | Apache-2.0 | Production | Permissive |
| AndroidX Activity Compose | `androidx.activity:activity-compose` | 1.10.1 | Apache-2.0 | Production | Permissive |
| AndroidX Navigation Compose | `androidx.navigation:navigation-compose` | 2.8.8 | Apache-2.0 | Production | Permissive |
| Compose BOM & UI | `androidx.compose:*` | 2025.02.00 | Apache-2.0 | Production | Permissive |
| AndroidX Room | `androidx.room:*` | 2.6.1 | Apache-2.0 | Production | Permissive |
| JUnit | `junit:junit` | 4.13.2 | EPL-1.0 | Test-only | **Test-only exception** (Covered by ADR 007; not shipped in release binaries). |
| Google Truth | `com.google.truth:truth` | 1.4.2 | Apache-2.0 | Test-only | Permissive |
| Kotlinx Coroutines Test | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | 1.10.1 | Test-only | Permissive |
| Robolectric | `org.robolectric:robolectric` | 4.14.1 | MIT | Test-only | Permissive |
| AndroidX Test Core | `androidx.test:core` | 1.6.1 | Apache-2.0 | Test-only | Permissive |

---

## 3. Licence Compliance Summary

- **Production / Shipped Binaries:** 100% Apache-2.0 and MIT (permissive). **Zero GPL, AGPL, or LGPL dependencies.**
- **Test-Only Exceptions:** JUnit 4 is licensed under EPL-1.0 and is restricted strictly to `testImplementation` scope (formalized in ADR 007). It is excluded from production release packaging.
