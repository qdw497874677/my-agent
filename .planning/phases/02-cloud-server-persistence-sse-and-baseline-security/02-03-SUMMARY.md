---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 03
subsystem: api
tags: [client-dto, run-events, sse, event-history, contracts]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Framework-free RunEvent envelope, event payload taxonomy, visibility, redaction, and sequence contracts.
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Phase 2 Maven and client-module verification foundation.
provides:
  - Public provider-neutral RunEvent DTO envelope for SSE and event history REST responses.
  - Public redaction metadata DTO for emitted and replayed run events.
  - Event history response DTO with run-scoped sequence replay cursor fields.
affects: [persistence, sse, rest-api, adapter-web, e2e]

tech-stack:
  added: []
  patterns: [Java record DTOs in pi-agent-client, contract tests for public API shape, domain-import isolation checks]

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RedactionDto.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/EventHistoryResponse.java
    - pi-agent-client/src/test/java/io/github/pi_java/agent/client/RunEventDtoContractTest.java
  modified: []

key-decisions:
  - "Keep event history REST and SSE on one shared RunEventDto envelope in pi-agent-client."
  - "Keep client event DTOs provider-neutral and free of Domain imports so adapter-web owns Domain-to-public mapping later."
  - "Expose replay through run-scoped sequence cursors afterSequence and nextAfterSequence."

patterns-established:
  - "Public client DTO records use JSON-friendly String IDs/wire names while preserving Phase 1 event identity and correlation fields."
  - "Client contract tests verify exact record component names for downstream REST/SSE compatibility."

requirements-completed: [CLOUD-02, CLOUD-03, CLOUD-05, E2E-05]

duration: 2m 24s
completed: 2026-06-14
---

# Phase 02 Plan 03: Define Public RunEvent DTO and Event-History Contract Summary

**Provider-neutral RunEvent DTO envelope shared by persisted event history and SSE replay, with redaction metadata and run-scoped sequence cursors.**

## Performance

- **Duration:** 2m 24s
- **Started:** 2026-06-14T05:04:15Z
- **Completed:** 2026-06-14T05:06:39Z
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments

- Added `RunEventDto` as the public event envelope preserving event ID, tenant/user/session/run/step/workspace context, sequence, timestamp, type, trace/correlation/causation IDs, visibility, redaction, payload schema/version, and payload.
- Added `RedactionDto` and `EventHistoryResponse` so REST history and SSE replay can share the same event representation with explicit cursor semantics.
- Added contract tests for exact envelope field names, run-scoped history cursors, and client DTO isolation from Domain types.

## Task Commits

Each task was committed atomically:

1. **Task 1: Define event DTOs shared by REST history and SSE** - `9807a59` (feat)

**Plan metadata:** recorded in the final docs commit for this plan.

_Note: The TDD task produced one implementation commit after the RED verification failed as expected against missing DTO classes._

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java` - Public event envelope DTO for event history and SSE streaming.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RedactionDto.java` - Public redaction metadata DTO.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/EventHistoryResponse.java` - Public event history response with sequence replay cursor fields.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/RunEventDtoContractTest.java` - Contract tests for event DTO fields, cursor semantics, and Domain import isolation.

## Decisions Made

- Kept event history REST and SSE on one shared `RunEventDto` envelope in `pi-agent-client` to avoid divergent public contracts.
- Kept client event DTOs provider-neutral and free of Domain imports so `pi-agent-adapter-web` can own Domain-to-public mapping later.
- Exposed replay through run-scoped sequence cursors `afterSequence` and `nextAfterSequence`, matching Phase 2 SSE replay decisions.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - no placeholder, TODO/FIXME, or hardcoded empty UI data-source stubs were introduced in the created event DTO files.

## Issues Encountered

- RED verification failed as expected because the new event DTO classes did not exist yet. The compiler also saw pre-existing 02-02 client API contract tests from the parallel execution context, but those files were outside this plan's scope and were not modified by this executor.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Persistence and adapter-web plans can map stored Domain `RunEvent` values into the public `RunEventDto` without exposing Domain record internals.
- SSE replay can use `EventHistoryResponse` cursor semantics and stream the same `RunEventDto` instances used by REST event history.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client -am -Dtest=RunEventDtoContractTest`

## Self-Check: PASSED

- Found created files: `RunEventDto.java`, `RedactionDto.java`, `EventHistoryResponse.java`, `RunEventDtoContractTest.java`, and this summary file.
- Found task commit: `9807a59`.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
