---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 02
subsystem: api
tags: [java, dto, rest, session, run, tdd]

requires:
  - phase: 01-runtime-spine-workspace-and-domain-contracts
    provides: Provider-neutral RunInput, RunStatus, Session, and Run boundary contracts
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Phase 2 dependency and verification foundation from Plan 01
provides:
  - Public client REST DTO records for API envelopes, pages, errors, sessions, runs, cancellation, details, and results
  - Contract tests guarding generic run input and client/domain separation
affects: [cloud-server-api, adapter-web, app-use-cases, sse-contracts, future-clients]

tech-stack:
  added: []
  patterns: [Java records for public DTOs, TDD contract-first client API boundary, Domain-free client contracts]

key-files:
  created:
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/ApiResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/ErrorResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/PageResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/CreateSessionRequest.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionHistoryResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CreateRunRequest.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunStatusResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CancelRunRequest.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunDetailResponse.java
    - pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResultResponse.java
    - pi-agent-client/src/test/java/io/github/pi_java/agent/client/CloudApiDtoContractTest.java
  modified: []

key-decisions:
  - "Keep pi-agent-client DTOs as plain Java records without Domain imports or Spring/Jakarta annotations."
  - "Represent create-run input generically with inputType plus Map input instead of chat-transcript-only API shape."

patterns-established:
  - "Client API DTOs live in pi-agent-client and are separated from framework-free Domain contracts."
  - "Session-centric run responses expose both sessionId and runId for REST paths and first-class run traceability."

requirements-completed: [CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05, E2E-01]

duration: 2m 42s
completed: 2026-06-14
---

# Phase 02 Plan 02: Define Public Session/Run REST DTO Contracts Summary

**Session-centric REST DTO records with generic run input, cancellation/detail/result envelopes, and contract tests that keep client APIs Domain-free.**

## Performance

- **Duration:** 2m 42s
- **Started:** 2026-06-14T05:04:09Z
- **Completed:** 2026-06-14T05:06:51Z
- **Tasks:** 1 completed
- **Files modified:** 13

## Accomplishments

- Added public response envelope, error, and pagination DTO records under `pi-agent-client`.
- Added session request/response/history DTO records for the Session-centric REST API boundary.
- Added run create/status/detail/result/cancel DTO records that preserve `sessionId`, `runId`, trace/correlation IDs, and generic `inputType`/`input` payload shape.
- Added TDD contract tests confirming create-run is not chat-only, run responses expose session/run IDs, and client DTOs do not import Domain types.

## Task Commits

Each task was committed atomically:

1. **Task 1 RED: Define public REST DTO records** - `a24b043` (test)
2. **Task 1 GREEN: Define public REST DTO records** - `8cda62a` (feat)

_Note: This was a TDD task, so it intentionally has separate test and implementation commits._

## Files Created/Modified

- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/ApiResponse.java` - Generic API response envelope carrying data, error, trace ID, and correlation ID.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/ErrorResponse.java` - Public error DTO with code, message, and details map.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/api/PageResponse.java` - Cursor-style page DTO using run-event sequence cursors.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/CreateSessionRequest.java` - Session creation request with workspace and metadata.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionResponse.java` - Session response with tenant/user/session/workspace/current-entry/status metadata.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/session/SessionHistoryResponse.java` - Session history response with temporary map-based entries.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CreateRunRequest.java` - Generic create-run request with `agentId`, `inputType`, `input`, workspace, and metadata.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResponse.java` - Run response with tenant/user/session/run/workspace/status and correlation fields.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunStatusResponse.java` - Run status response with terminal marker and correlation fields.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CancelRunRequest.java` - Run cancellation request with reason.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunDetailResponse.java` - Aggregate run detail response for events, steps, messages, tool calls, and result.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/RunResultResponse.java` - Terminal run result/failure DTO.
- `pi-agent-client/src/test/java/io/github/pi_java/agent/client/CloudApiDtoContractTest.java` - Client DTO contract tests.

## Decisions Made

- Kept DTO records intentionally plain: no Domain imports, no Spring/Jakarta annotations, and no persistence/provider types.
- Used `inputType` + `Map<String,Object> input` to keep the REST create-run contract aligned with generic Domain `RunInput` modes without exposing Domain sealed interfaces.
- Left event/detail sub-lists as `List<Map<String,Object>>` for this plan because Plan 03 is responsible for introducing explicit public RunEvent DTOs.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - stub scan found no TODO/FIXME/placeholder/hardcoded empty UI data sources in the created DTO/test files.

## Issues Encountered

- The expected TDD RED step failed at test compilation because the DTO packages did not exist yet. This was the intended failing contract before implementation.
- Parallel executor activity is present in the working tree and recent git history for other Phase 02 plans. No unrelated files were staged or committed by this executor.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-client -am -Dtest=CloudApiDtoContractTest`
- Acceptance checks confirmed `CreateRunRequest` has `String inputType` and `Map<String,Object> input`, does not contain `chatTranscript`, run responses expose `sessionId` and `runId`, and client DTOs do not import Domain packages.

## Next Phase Readiness

- Adapter/App plans can consume stable client DTO names for session-centric REST endpoints.
- Plan 03 can replace temporary map-based event/detail list contracts with explicit public RunEvent DTOs while preserving this plan's envelope and pagination patterns.

## Self-Check: PASSED

- Verified all 13 created DTO/test files and this SUMMARY file exist.
- Verified task commits exist: `a24b043`, `8cda62a`.
- Note: first self-check attempt used unavailable `rg`; reran successfully with `git rev-parse --verify`.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
