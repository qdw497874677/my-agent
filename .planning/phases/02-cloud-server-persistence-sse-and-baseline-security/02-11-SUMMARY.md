---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 11
subsystem: api
tags: [spring-mvc, sse, run-events, replay, fanout, cloud-server]

# Dependency graph
requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Run query/event-history API, RunEventDto mapping, persist-then-emit fanout port
provides:
  - Explicit per-run SSE live subscription fanout with close-once subscription lifecycle
  - Replay-before-subscribe Spring MVC SseEmitter endpoint for run event streams
  - Sequence-based reconnect support using afterSequence and Last-Event-ID
  - Subscriber cleanup tests for unsubscribe, send failure, emitter lifecycle, and terminal events
affects: [phase-02, cloud-server, sse, persistence, event-streaming, e2e]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Persisted event history is replayed before live in-process subscription
    - SseEmitter event id is the per-run event sequence and event name is RunEventDto.type
    - Fanout remains in-memory only and never owns replay buffers or JDBC dependencies

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunEventStreamController.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseSubscription.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/RunEventStreamService.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunSseIntegrationTest.java
  modified: []

key-decisions:
  - "Keep live SSE fanout in Adapter Web as an in-memory subscriber registry while durable replay remains owned by RunQueryService/event persistence."
  - "Use bare per-run RunEvent.sequence as the SSE id and parse Last-Event-ID only when it is a positive long."
  - "Complete SSE emitters after terminal run events and close subscriptions from completion, timeout, error, send failure, and terminal paths."

patterns-established:
  - "Replay-before-subscribe: RunEventStreamService calls RunQueryService.listEvents before SseRunEventFanout.subscribe to avoid missing persisted events."
  - "Close-once subscription: SseSubscription wraps unsubscribe with AtomicBoolean to tolerate multiple emitter lifecycle callbacks."
  - "Per-run fanout isolation: subscribers are stored by runId and live publish only delivers events matching event.runId()."

requirements-completed: [CLOUD-02, CLOUD-03, CLOUD-05, E2E-05]

# Metrics
duration: 5m 38s
completed: 2026-06-14
---

# Phase 02 Plan 11: SSE Replay and Subscription Lifecycle Summary

**Spring MVC SSE run-event streaming with persisted replay before live per-run fanout subscription and deterministic subscriber cleanup.**

## Performance

- **Duration:** 5m 38s
- **Started:** 2026-06-14T05:54:28Z
- **Completed:** 2026-06-14T06:00:06Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Added `SseSubscription` and `SseRunEventFanout` so live run-event subscribers have explicit IDs, close-once unsubscribe semantics, per-run isolation, and send-failure cleanup.
- Added `RunEventStreamService` and `RunEventStreamController` for `GET /api/sessions/{sessionId}/runs/{runId}/stream` with `text/event-stream`, `afterSequence`, and `Last-Event-ID` support.
- Ensured SSE replay uses persisted `RunQueryService.listEvents(...)` results before subscribing to live fanout, preserving reconnect correctness and persist-then-emit assumptions.
- Covered fanout subscription behavior, replay ordering, `Last-Event-ID` cursor override, SSE id sequence mapping, and terminal-event cleanup in `RunSseIntegrationTest`.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement explicit SSE subscription fanout contract** - `cad59b0` (feat)
2. **Task 2: Implement replay-before-subscribe SSE endpoint** - `6040852` (feat)

**Plan metadata:** pending final docs commit

_Note: Task plans were marked TDD. Red test state was observed before implementation for each task, then green verification passed before commit._

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunEventStreamController.java` - Spring MVC SSE endpoint for session-centric run event streams.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseSubscription.java` - Close-once live subscription handle with run and subscriber IDs.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java` - In-memory per-run fanout implementing Infrastructure `RunEventFanout` without persistence dependencies.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/RunEventStreamService.java` - Replay-before-subscribe orchestration, SSE event formatting, cursor resolution, and emitter lifecycle cleanup.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/RunSseIntegrationTest.java` - Fanout and endpoint integration tests for Plan 02-11 behavior.

## Decisions Made

- Keep `SseRunEventFanout` in Adapter Web because it is Spring/SSE delivery infrastructure, while the durable event log remains queried through App `RunQueryService` and persisted through Infrastructure.
- Use per-run numeric sequence as SSE `id` to directly align reconnect cursors with `afterSequence` and `Last-Event-ID`.
- Expose `subscriberCount(String runId)` as a diagnostic/test-visible method so cleanup can be verified without exposing internal maps or adding a new production dependency.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None found in created/modified production files. Test mocks in `RunSseIntegrationTest` are intentional test doubles for `RunQueryService` and other Spring Boot collaborators.

## Issues Encountered

- TDD red runs failed as expected because the SSE package/controller/service did not exist yet.
- `subscriberCount` needed to be public for integration-test cleanup assertions across packages; this is a small diagnostic surface and does not expose subscriber details.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunSseIntegrationTest` — passed.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-12 can wire `PersistingEventSink -> DB -> SseRunEventFanout` through Spring composition with the live fanout contract already in place.
- E2E reconnect/replay tests can rely on persisted event history plus live fanout rather than an in-memory replay buffer.

## Self-Check: PASSED

- Found created files: `RunEventStreamController.java`, `SseSubscription.java`, `SseRunEventFanout.java`, `RunEventStreamService.java`, and `RunSseIntegrationTest.java`.
- Found task commits: `cad59b0` and `6040852`.
- Verification command passed after both task commits.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
