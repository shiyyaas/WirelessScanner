# Contributing to WirelessScanner

Thank you for contributing to **WirelessScanner**! Please review our core guidelines and workflow rules before submitting contributions.

---

## Core Guidelines & Non-Negotiables

1. **Passive Observation Only**: The application and any supported hardware are strictly for lawful, passive observation. Deauthentication, spoofing, packet injection, active interference, or intrusion are strictly forbidden.
2. **Provenance & Evidence**: Every observation must record its receiver source, timestamp, raw identifier, and signal strength. Evidence is never synthesized.
3. **No Distance or Direction Claims**: RSSI alone must never be presented as physical distance or direction.
4. **Address Randomization Awareness**: Never assume matching addresses or names represent the same physical device across time or radios.
5. **Classifications as Hypotheses**: Device classifications must carry confidence scores and evidence trails, never presenting attribution as absolute fact.
6. **Strict Dependency Licensing Policy**: Only permissively licensed dependencies (**MIT, BSD, Apache-2.0**) are permitted in production builds. GPL and AGPL dependencies are strictly excluded. LGPL requires an explicit Architecture Decision Record (ADR) prior to introduction.
7. **Privacy by Default**: All data is processed locally. No analytics or telemetry uploads are permitted.

---

## Contribution Workflow

1. **Branching**: Never push directly to `main`. Create feature or documentation branches from `main`.
2. **Conventional Commits**: Use clear, conventional commit messages (e.g., `feat: ...`, `fix: ...`, `docs: ...`).
3. **Verification**: Always verify changes locally before opening a pull request:
   - `./gradlew :core:model:test`
   - `./gradlew :app:testDebugUnitTest`
   - `./gradlew :app:assembleDebug`
4. **Pull Requests**: Open pull requests via `gh pr create` with a detailed description explaining what, why, roadmap impact, evidence, and verification steps.
