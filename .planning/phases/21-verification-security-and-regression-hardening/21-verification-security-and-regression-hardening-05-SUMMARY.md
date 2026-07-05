---
phase: 21-verification-security-and-regression-hardening
plan: 05
subsystem: testing
tags: [java, vaadin, streaming, regression, release-hardening, ver-05]

# Dependency graph
requires:
  - phase: 18-streaming-bubble-lifecycle
    provides: Reducer and ChatEventStreamPanel one-bubble streaming semantics
  - phase: 21-verification-security-and-regression-hardening
    provides: Phase 21 release-hardening documentation and prior verification gates
provides:
  - Deterministic slow-stream component proof that partial assistant text appears before terminal completion
  - VER-05 release-hardening command documented for no-key Java verification
affects: [phase-21, release-hardening, console-streaming, verification]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Controlled reducer/panel test stream with explicit before-terminal checkpoint
    - Release-hardening docs organized by VER gate command

key-files:
  created:
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSlowStreamIncrementalTest.java
  modified:
    - docs/phase-21-verification-hardening.md

key-decisions:
  - "VER-05 uses the public ConversationEventReducer and ChatEventStreamPanel component seams instead of timing sleeps or provider/network calls."
  - "The slow-stream proof includes an explicit negative completion-only checkpoint so buffering-until-completion cannot satisfy the gate."

patterns-established:
  - "Controlled slow stream: drain deltas until a before-terminal marker, assert visible partial text, then drain completion and assert same bubble completion."
  - "Release hardening gate docs must include exact copy/paste Java 21 Maven commands and the failure mode they protect."

requirements-completed: [VER-05]

# Metrics
duration: 5m30s
completed: 2026-07-05
---

# Phase 21 Plan 05: Incremental Slow Stream Summary

**Deterministic Vaadin reducer/panel proof that fake slow-stream assistant partial text is visible before terminal completion.**

## Performance

- **Duration:** 5m30s
- **Started:** 2026-07-05T06:59:50Z
- **Completed:** 2026-07-05T07:05:20Z
- **Tasks:** 2
- **Files modified:** 2

## Accomplishments

- Added `WebConsoleSlowStreamIncrementalTest` with a controlled slow-stream sequence that emits a `model.delta`, pauses at a before-terminal checkpoint, and proves the assistant bubble already contains partial text before `run.completed` is processed.
- Verified the same primary assistant bubble transitions from `streaming`/`pending` to `completed` after terminal completion without creating a replacement bubble.
- Added a completion-only negative guard showing that no partial text is visible at the before-terminal checkpoint when only terminal completion is delivered.
- Documented the VER-05 release-hardening command and stated that the gate must fail if assistant text is only replayed after completion.

## Task Commits

Each task was committed atomically:

1. **Task 1: Add controlled slow-stream incremental component proof** - `5c81b87` (test)
2. **Task 2: Add VER-05 command to release-hardening docs** - `b4ae822` (docs)

**Plan metadata:** Pending final metadata commit.

## Files Created/Modified

- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSlowStreamIncrementalTest.java` - Controlled reducer/panel slow-stream proof for VER-05, including before-terminal partial assertion and terminal completion mutation assertion.
- `docs/phase-21-verification-hardening.md` - VER-05 gate section with exact Maven command and buffering-after-completion failure condition.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleSlowStreamIncrementalTest test` — passed after Task 1.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleSlowStreamIncrementalTest test` — passed after Task 2.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleSlowStreamIncrementalTest test` — passed as final gate.

## Decisions Made

- VER-05 uses the public `ConversationEventReducer` and `ChatEventStreamPanel` component seams instead of timing sleeps or provider/network calls, keeping the gate deterministic and always runnable.
- The slow-stream proof includes an explicit completion-only negative checkpoint so buffering-until-completion cannot satisfy the incremental-before-terminal requirement.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Renamed record factory to avoid accessor collision**
- **Found during:** Task 1 (Add controlled slow-stream incremental component proof)
- **Issue:** The initial `SlowFrame.beforeTerminal()` static factory conflicted with the record accessor name, causing Java compilation errors.
- **Fix:** Renamed the factory to `beforeTerminalMarker()` while preserving the explicit before-terminal checkpoint behavior.
- **Files modified:** `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSlowStreamIncrementalTest.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-adapter-web -am -Dtest=WebConsoleSlowStreamIncrementalTest test`
- **Committed in:** `5c81b87` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Compile-only correction needed for the planned test artifact; no scope expansion.

## Issues Encountered

- Initial Task 1 verification failed at test compilation because a record static factory name collided with its generated accessor; fixed inline and reran the planned gate successfully.
- Task 2 documentation text had already been applied by the time of the first commit attempt, likely due concurrent Phase 21 execution context; added a small alignment sentence and committed the task-related docs change atomically.

## Known Stubs

None. The created test has no placeholder or mock-only UI stubs that prevent VER-05 from being satisfied. The `frame.partial() != null` check is an intentional optional event-frame field in test control data, not a UI/data-source stub.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- VER-05 now has an always-runnable no-key Java gate for incremental slow-stream semantics.
- Phase 21 release-hardening docs now include the VER-05 command alongside the other verification gates.
- No blockers remain for this plan.

## Self-Check: PASSED

- FOUND: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/WebConsoleSlowStreamIncrementalTest.java`
- FOUND: `.planning/phases/21-verification-security-and-regression-hardening/21-verification-security-and-regression-hardening-05-SUMMARY.md`
- FOUND: Task 1 commit `5c81b87`
- FOUND: Task 2 commit `b4ae822`

---
*Phase: 21-verification-security-and-regression-hardening*
*Completed: 2026-07-05*
