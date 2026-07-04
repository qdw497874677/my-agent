---
phase: 20-provider-model-and-local-profile-stability
plan: 05
subsystem: testing
tags: [java, vaadin, sqlite, local-profile, provider-model, restart-recovery]

requires:
  - phase: 20-provider-model-and-local-profile-stability-02
    provides: Provider/model selection persistence, no-provider blocking, and explicit fallback UI labels
  - phase: 20-provider-model-and-local-profile-stability-03
    provides: Safe RunProviderMetadata persistence contracts for JDBC and SQLite
  - phase: 20-provider-model-and-local-profile-stability-04
    provides: Selected model snapshots and fallback facts wired into run creation/dispatch/runtime
provides:
  - Same-DB SQLite restart recovery proof for provider config, selected model, recent sessions, typed transcript, run metadata, fallback facts, and ownership filters
  - Focused final Phase 20 Java regression gate covering model bar selectors, no-key/fallback behavior, selected-model next-run metadata, and restart recovery
  - Phase 21 handoff documentation for stable selectors, safe metadata fields, fallback semantics, restart commands, and deferred gaps
affects: [phase-21-verification-security-and-regression-hardening, local-profile, provider-model-ux]

tech-stack:
  added: []
  patterns:
    - Same SQLite DB restart proof by recreating persistence/store/query/config objects
    - Local profile event rehydration fallback for persisted typed RunEvent JSON
    - Focused no-key Java gate with explicit Java 21 Maven invocation

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java
    - docs/phase-20-provider-model-local-profile.md
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java

key-decisions:
  - "Same-DB restart proof recreates SqliteLocalPersistence, LocalDevStores, repositories, DefaultConversationQueryService, and ProviderConfigStore instead of reusing in-memory maps or Vaadin component state."
  - "LocalDevStores now reconstructs persisted model-delta events after SQLite restart when direct Jackson polymorphic RunEvent deserialization is not available."
  - "Phase 21 handoff keeps deferred ideas explicit: no automatic paid-provider fallback, no multi-provider routing, no provider-specific context-window policy, and no conversation search/rename/archive/pin/delete."

patterns-established:
  - "Restart tests seed through provider config and local repository/event-store seams, then discard objects and reload from the same SQLite path."
  - "Focused provider/model gate asserts stable data-* selectors and safe RunProviderMetadata snapshots rather than browser timing or screenshots."

requirements-completed: [SESS-05, PROV-01, PROV-02, PROV-03, PROV-04, PROV-05, PROV-06]
duration: 623s
completed: 2026-07-04
---

# Phase 20 Plan 05: Local Profile Restart Recovery Summary

**Same-DB SQLite restart recovery proof for provider config, selected model, typed transcript, run metadata, explicit fallback labels, and ownership filters**

## Performance

- **Duration:** 623s
- **Started:** 2026-07-04T11:52:23Z
- **Completed:** 2026-07-04T12:02:46Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- Added `LocalProfileRestartRecoveryTest`, proving that local profile restart reconstructs fresh SQLite persistence/store/query/config objects against the same DB file and recovers provider config, selected model, recent sessions, typed transcript, run metadata, fallback metadata, and ownership filters.
- Fixed local profile event hydration so model delta events persisted to SQLite can be reconstructed into typed `RunEvent` instances after restart and included in `ConversationQueryService.getTranscript(...)` output.
- Added a final focused provider/model regression assertion proving the selected model snapshot is copied into the next run request metadata.
- Documented Phase 20 selector contracts, refresh states, safe run metadata fields, fallback semantics, restart verification commands, and Phase 21 handoff gaps.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add same-DB local profile restart recovery proof**
   - `dd91015` (test): added the failing TDD restart recovery proof.
   - `5907b20` (fix): implemented local event rehydration needed for the proof to pass.
2. **Task 2: Add final focused regression gate and Phase 21 handoff documentation**
   - `31a4b23` (test): added selected-model next-run metadata assertion and Phase 21 handoff docs.

**Plan metadata:** captured by the final docs commit for this plan.

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java` - Same-DB local profile restart integration proof for provider config/model, sessions, transcript, metadata, fallback labels, and ownership filters.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java` - Rehydrates persisted model-delta events when local stores load from SQLite after restart.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleProviderModelBarTest.java` - Adds selected-model next-run metadata regression coverage.
- `docs/phase-20-provider-model-local-profile.md` - Documents stable selectors, safe metadata fields, fallback semantics, restart commands, Phase 21 gaps, and deferred ideas.

## Decisions Made

- Same-DB restart proof recreates `SqliteLocalPersistence`, `LocalDevStores`, repositories, `DefaultConversationQueryService`, and `ProviderConfigStore` instead of reusing in-memory maps or Vaadin component state.
- LocalDevStores reconstructs persisted model-delta events after SQLite restart when direct Jackson polymorphic `RunEvent` deserialization is not available.
- Phase 21 handoff keeps deferred ideas explicit: no automatic paid-provider fallback, no multi-provider routing, no provider-specific context-window policy, and no conversation search/rename/archive/pin/delete.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Rehydrated SQLite-persisted run events for restart transcripts**
- **Found during:** Task 1 (Add same-DB local profile restart recovery proof)
- **Issue:** The restart test recovered the run input/user message but not the assistant message because `LocalDevStores.loadAll()` attempted direct Jackson deserialization of polymorphic `RunEvent` JSON and silently skipped persisted events.
- **Fix:** Added a conservative fallback parser that reconstructs model-delta `RunEvent` instances from the stored event row and payload map while preserving tenant/user/session/run ownership fields.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/LocalDevRuntimeBeanConfiguration.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=LocalProfileRestartRecoveryTest test`
- **Committed in:** `5907b20`

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Required for correctness of the planned SQLite restart proof; no scope creep.

## Issues Encountered

- Maven defaulted to Java 17 in this environment (`mvn -version`), causing `release version 21 not supported`. Verification used `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64` to match the project Java 21 baseline.
- Running `mvn -pl pi-agent-adapter-web` without `-am` exposed stale/missing reactor dependency classes in test compile. Verification used `-am` so required modules were built in the same reactor.
- Pre-existing unrelated working tree changes were observed and intentionally left untouched: `.gitignore` and `.planning/phases/17-console-session-restore-ux/17-VERIFICATION.md`.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=LocalProfileRestartRecoveryTest test` — passed (4 tests).
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleProviderModelBarTest,WebConsoleNoProviderFallbackTest,LocalProfileRestartRecoveryTest test` — passed (17 tests).

## Known Stubs

None. The only empty-string fallback found is a test metadata helper that omits a nullable safe error summary; it does not flow to production UI rendering.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Phase 20 focused gates are ready for Phase 21 to broaden into release/security/browser regression.
- Stable selector and safe metadata contracts are documented for browser and security verification.
- Phase 21 should keep Java 21 explicit in local Maven commands if Maven continues to default to Java 17.

## Self-Check: PASSED

- Found `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/LocalProfileRestartRecoveryTest.java`.
- Found `docs/phase-20-provider-model-local-profile.md`.
- Found `.planning/phases/20-provider-model-and-local-profile-stability/20-provider-model-and-local-profile-stability-05-SUMMARY.md`.
- Found task commits `dd91015`, `5907b20`, and `31a4b23` in recent git history.

---
*Phase: 20-provider-model-and-local-profile-stability*
*Completed: 2026-07-04*
