---
phase: 21-verification-security-and-regression-hardening
plan: 01
subsystem: testing
tags: [java, vaadin, regression, provider-model, streaming, redaction]

# Dependency graph
requires:
  - phase: 17-console-session-restore-ux
    provides: Console recent-session restore and same-session continuation seams
  - phase: 18-streaming-bubble-lifecycle
    provides: ConversationEventReducer and one-bubble streaming semantics
  - phase: 20-provider-model-and-local-profile-stability
    provides: provider/model readiness and run metadata snapshot contracts
provides:
  - Named deterministic VER-01 Java regression gate for conversation semantics
  - Documentation for the focused VER-01 release command and behavior mapping
affects: [phase-21, release-verification, console-regression]

# Tech tracking
tech-stack:
  added: []
  patterns: [deterministic no-key Vaadin component gate, fake provider/model snapshot assertion, reducer-level streaming release gate]

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase21ConversationRegressionGateTest.java
  modified:
    - docs/phase-21-verification-hardening.md

key-decisions:
  - "Use one named adapter-web Java gate as the VER-01 release entry point, reusing existing Console/provider/streaming seams instead of adding network/provider dependencies."
  - "Keep the configured-provider path deterministic by asserting safe run request metadata snapshots from fake provider/model config rather than real provider credentials."

patterns-established:
  - "Phase 21 regression gates should prove behavior through stable semantic component attributes and App/client DTO metadata, not screenshots or provider SDK state."
  - "Provider error coverage checks safe actionable copy and redaction of secret-looking strings without depending on real upstream providers."

requirements-completed: [VER-01]

# Metrics
duration: 10m24s
completed: 2026-07-05
---

# Phase 21 Plan 01: VER-01 Conversation Regression Gate Summary

**Named no-key Java regression gate proving fallback, configured-provider snapshots, restore, continuation, streaming coalescing, cancellation/error, and provider-error redaction**

## Performance

- **Duration:** 10m24s
- **Started:** 2026-07-05T06:43:29Z
- **Completed:** 2026-07-05T06:53:53Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `Phase21ConversationRegressionGateTest` with seven explicitly named VER-01 behavior labels: `noKeyFallback`, `configuredProviderPath`, `recentSessionRestore`, `sameSessionContinuation`, `streamingCoalescing`, `cancellationAndErrorStates`, and `providerErrors`.
- Proved the configured-provider path with fake ready provider/model config and safe run metadata snapshot assertions (`selectedModelRef`, `resolvedProviderId`, `resolvedModelId`, `fallbackMode`, `readinessState`) without provider keys or external network.
- Documented the focused VER-01 release command and mapped each required behavior to its test method in `docs/phase-21-verification-hardening.md`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add named Phase 21 conversation regression gate** - `254679b` (test)
2. **Task 2: Document the consolidated release regression command** - `0fe4665` (docs)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase21ConversationRegressionGateTest.java` - Named Java release regression gate covering all VER-01 conversation behaviors with deterministic fake seams.
- `docs/phase-21-verification-hardening.md` - Documents the VER-01 command, coverage table, and release gate sequencing.

## Decisions Made

- Use one named adapter-web Java gate as the VER-01 release entry point, reusing existing Console/provider/streaming seams instead of adding network/provider dependencies.
- Keep the configured-provider path deterministic by asserting safe run request metadata snapshots from fake provider/model config rather than real provider credentials.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Test assertion bug] Aligned provider-error assertion with existing safe error surface**
- **Found during:** Task 1 (Add named Phase 21 conversation regression gate)
- **Issue:** The initial `providerErrors` assertion expected the raw error field to contain the word `check`, but the existing safe provider controller returns actionable provider copy in `message()` and a redacted HTTP status/body summary in `error()`.
- **Fix:** Kept the actionable provider assertion on `message()` and changed the `error()` assertion to verify the safe redacted service-unavailable summary.
- **Files modified:** `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase21ConversationRegressionGateTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=Phase21ConversationRegressionGateTest test`
- **Committed in:** `254679b`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Assertion was corrected to match existing safe behavior; no scope expansion or production changes.

## Issues Encountered

- The focused test initially failed on the provider-error copy assertion and passed after aligning the assertion with existing redacted provider-controller behavior.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=Phase21ConversationRegressionGateTest test` — passed, 7 tests.
- `grep -R "OPENAI_API_KEY\|api.openai.com" pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase21ConversationRegressionGateTest.java` — no matches.

## Known Stubs

None - scanned created/modified plan files for placeholder/TODO/stub patterns and found no goal-blocking stubs.

## Self-Check: PASSED

- Found `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/Phase21ConversationRegressionGateTest.java`.
- Found `docs/phase-21-verification-hardening.md`.
- Found `.planning/phases/21-verification-security-and-regression-hardening/21-verification-security-and-regression-hardening-01-SUMMARY.md`.
- Found task commit `254679b` in `git log --oneline --all -10`.
- Found task commit `0fe4665` in `git log --oneline --all -10`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- VER-01 is complete and can be used as the deterministic no-key Java release gate.
- Remaining Phase 21 plans can build on the documented verification file for ownership, architecture, browser, and slow-stream gates.

---
*Phase: 21-verification-security-and-regression-hardening*
*Completed: 2026-07-05*
