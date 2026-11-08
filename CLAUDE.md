# WirelessScanner: rules for Claude Code

ROADMAP.md is the source of truth for scope and order. Read its Guiding Constraints,
Non-Goals, and Definition of Done before changing anything.

## Non-negotiables
- Passive observation only. No deauth, spoofing, injection, or interception.
- Never present RSSI as distance or direction. Never merge observations across radios by address or name.
- Classifications are hypotheses with evidence and confidence, never facts.
- Dependencies: MIT, BSD, or Apache-2.0 only. No GPL/AGPL. LGPL needs an ADR. Record name, version, licence in THIRD_PARTY_NOTICES.
- No network upload, no analytics, location only with explicit opt-in, databases excluded from backup.
- Do not copy code from other projects. If you find code that looks copied, report it; do not fix it silently.

## Workflow
- Builds and releases run remotely in GitHub Actions. Do not assume a local Android SDK.
- Never push to main, never force-push, never create tags or releases unless asked.
- One logical change per PR. Conventional Commits. Open PRs with `gh pr create`.
- Verify through CI: `gh pr checks`, `gh run watch`, and on failure `gh run view --log-failed`.
  Stop and report after 3 failed fix attempts for the same failure.
- Never print, log, or edit secrets or signing configuration.
- Tick a roadmap checkbox only when the PR description cites evidence (file, test, or CI run).
- Update docs in the same PR as the code they describe.
- If unsure, mark it "unverified" and ask. Do not guess.
