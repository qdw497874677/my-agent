---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 05
subsystem: app
tags: [cola, app-ports, persistence, queue, cancellation, audit, tdd]
requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: Public client DTOs and App request/use-case boundaries from plans 02-02 through 02-04.
provides:
  - Framework-free App persistence, audit, queue, dispatcher, terminal-event publisher, and cancellation port contracts.
  - Broker-neutral queued cancellation contract returning QueuedRun context for terminal event publishing.
  - App port contract tests and App architecture verification coverage.
affects: [02-06, 02-07, 02-08, cloud-server, persistence, sse, cancellation]
tech-stack:
  added: []
  patterns: [COLA App ports, TDD contract-first port definition, idempotent terminal-event seams]
key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/AuditRepository.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunQueue.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunDispatcher.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunTerminalEventPublisher.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/CancellationRegistry.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunExecutionState.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/AppPortContractTest.java
  modified: []
key-decisions:
  - "Keep persistence and execution contracts in pi-agent-app as plain Java ports depending only on App, Client, and Domain types."
  - "Model queued cancellation as Optional<QueuedRun> cancelQueuedAndReturn(...) so App services can publish exactly one durable terminal event with the original queued context."
  - "Make RunTerminalEventPublisher methods publish*IfAbsent to reserve a durable RunEventStore.hasTerminalEvent guard for completed, cancelled, failed, and timed-out paths."
patterns-established:
  - "App ports are framework- and broker-neutral; Infrastructure will implement JDBC/PostgreSQL/queue mechanics later."
  - "Cancellation contracts explicitly separate durable queued/run state changes from active runtime CancellationToken signaling."
requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-04]
duration: 3m 01s
completed: 2026-06-14
---

# Phase 02 Plan 05: App Persistence, Queue, Dispatcher, and Cancellation Ports Summary

**Framework-free App contracts for durable run/session/event/audit persistence, broker-neutral execution queues, idempotent terminal events, and runtime cancellation signaling**

## Performance

- **Duration:** 3m 01s
- **Started:** 2026-06-14T05:16:18Z
- **Completed:** 2026-06-14T05:19:19Z
- **Tasks:** 1
- **Files modified:** 11

## Accomplishments

- Added App-layer persistence ports for run events, run projections/read models, sessions, and audit records without Spring/JDBC/infrastructure imports.
- Added execution ports for queued runs, queue claiming, queued cancellation with payload return, dispatcher entry points, terminal event publishing, and active cancellation token registry.
- Captured queued/running/terminal semantics in `RunExecutionState` and contract tests, including terminal idempotency and explicit queued cancellation behavior.
- Verified App COLA boundaries remain clean with `AppDependencyArchTest`.

## Task Commits

TDD task committed atomically in RED/GREEN steps:

1. **Task 1 RED: Define App port contracts as failing tests** - `ec11598` (test)
2. **Task 1 GREEN: Define persistence and execution ports** - `07ffc1f` (feat)

**Plan metadata:** Pending final docs commit.

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunEventStore.java` - Append-only run event store contract with replay, last event, and terminal-event detection.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java` - Run read-model/projection contract for create, status, cancellation request, idempotent terminal updates, detail, steps, messages, tool calls, and result queries.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/SessionRepository.java` - Session create/find/history persistence contract.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/AuditRepository.java` - Audit recording port carrying request, resource, session/run, and details context.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/QueuedRun.java` - Queue payload record preserving tenant/user/session/run/workspace/correlation/input context for worker and terminal-event paths.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunExecutionState.java` - Execution-state enum with terminal-state helper.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunQueue.java` - Broker-neutral queue contract with claim, running, terminal, queued cancellation return, and terminal cleanup operations.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunDispatcher.java` - Worker dispatch seam for polling or targeted dispatch.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/RunTerminalEventPublisher.java` - App-owned idempotent terminal-event publishing seam.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/execution/CancellationRegistry.java` - Active runtime token registry and cancellation signaling seam.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/port/AppPortContractTest.java` - Reflection/contract tests for port shape, cancellation semantics, terminal-event guards, and infrastructure import absence.

## Decisions Made

- Keep persistence and execution contracts in `pi-agent-app` as plain Java ports depending only on App, Client, and Domain types so downstream JDBC/PostgreSQL implementations stay in Infrastructure.
- Use `Optional<QueuedRun> cancelQueuedAndReturn(String runId, String reason, Instant cancelledAt)` rather than a boolean-only cancellation method because App services need queued context to publish a correct terminal `run.cancelled` event.
- Express terminal publication as `publish*IfAbsent` methods so later implementations must guard with `RunEventStore.hasTerminalEvent(run.runId())` or an equivalent durable check.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None - no placeholder/mock/TODO stubs were found in files created or modified by this plan.

## Issues Encountered

- TDD RED failed as expected because the App port packages did not exist yet.
- Existing unrelated planning files in the phase directory were already modified/untracked before this executor began; they were not touched or staged as part of task commits.

## User Setup Required

None - no external service configuration required.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest=AppPortContractTest,AppDependencyArchTest`

## Self-Check: PASSED

- Created files exist: `RunEventStore`, `RunProjectionRepository`, `SessionRepository`, `AuditRepository`, `QueuedRun`, `RunQueue`, `RunDispatcher`, `RunTerminalEventPublisher`, `CancellationRegistry`, `RunExecutionState`, and `AppPortContractTest`.
- Task commits exist: `ec11598` and `07ffc1f`.

## Next Phase Readiness

- Plan 02-06 can implement concrete App services against these ports, including create-run enqueueing and idempotent cancellation orchestration.
- Plans 02-07 and 02-08 can provide JDBC/PostgreSQL persistence, DB-backed queue, dispatcher/worker, and terminal-event publisher implementations without changing App contract shape.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
