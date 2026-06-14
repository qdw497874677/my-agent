---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 08
subsystem: infrastructure-execution
tags: [java, postgres, jdbc, queue, cancellation, dispatcher, worker, testcontainers]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App execution ports, terminal event publisher, JDBC persistence/event store contracts
provides:
  - PostgreSQL-backed RunQueue with SKIP LOCKED claiming and queued/claimed cancellation payload return
  - In-memory CancellationRegistry for active runtime cancellation tokens
  - Concrete DefaultRunDispatcher with AgentRuntime start/cancel, timeout, terminal fallback publishing, audit, and cleanup
  - Plain RunWorkerScheduler implementation without active Spring bean registration
affects: [phase-02-composition-root, phase-02-e2e, phase-03-runtime-provider-integration]

tech-stack:
  added: []
  patterns: [PostgreSQL row-lock queue, idempotent terminal fallback publishing, plain Infrastructure worker classes]

key-files:
  created:
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/queue/PostgresRunQueue.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/InMemoryCancellationRegistry.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/RunWorkerScheduler.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/queue/PostgresRunQueueTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/InMemoryCancellationRegistryTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherTest.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/RunWorkerSchedulerTest.java
  modified:
    - pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql

key-decisions:
  - "Keep worker and scheduler classes as plain Infrastructure classes; Spring bean registration remains deferred to the later composition-root plan."
  - "Extend run_queue schema with queued-run context/payload columns so cancellation and worker execution can publish terminal events from the original run context."
  - "Use AgentRuntime.start(context) behind a bounded worker Future so timeout handling can invoke AgentRuntime.cancel(runId, 'timeout') and mark TIMED_OUT consistently."

patterns-established:
  - "PostgresRunQueue claimNext uses FOR UPDATE SKIP LOCKED and transitions QUEUED -> CLAIMED with a 30-second lease."
  - "DefaultRunDispatcher checks RunEventStore.hasTerminalEvent before fallback terminal publishing to avoid duplicate terminal RunEvents."
  - "CancellationRegistry tokens are always removed in dispatcher finally blocks."

requirements-completed: [CLOUD-01, CLOUD-04, CLOUD-06, E2E-01, E2E-04]

duration: 7m 22s
completed: 2026-06-14
---

# Phase 02 Plan 08: DB Queue, Cancellation Registry, and Run Dispatcher Summary

**PostgreSQL SKIP LOCKED run queue with active cancellation tokens and a concrete worker dispatcher that drives AgentRuntime terminal orchestration**

## Performance

- **Duration:** 7m 22s
- **Started:** 2026-06-14T05:34:53Z
- **Completed:** 2026-06-14T05:42:15Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Implemented `PostgresRunQueue` with enqueue, atomic `claimNext` using `FOR UPDATE SKIP LOCKED`, `markRunning`, idempotent `markTerminal`, payload-returning queued cancellation, and terminal-only removal.
- Implemented `InMemoryCancellationRegistry` with `ConcurrentHashMap` token lifecycle and active cancellation signaling.
- Implemented `DefaultRunDispatcher` to claim queued runs, build `RunContext`, call `AgentRuntime.start(context)`, handle completed/cancelled/timed-out/failed outcomes, publish terminal fallback events only when absent, update projections/queue state, audit worker actions, and remove cancellation tokens.
- Implemented plain `RunWorkerScheduler` with `pollOnce()` and `triggerAsync()` and no `@Scheduled`, `@Component`, `@Configuration`, or `@Bean` ownership.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement PostgreSQL queue and cancellation registry** - `6494af5` (feat)
2. **Task 2: Implement concrete run dispatcher and worker terminal orchestration** - `3aa6bd7` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` - Adds queued-run context, input JSONB, and cancel reason columns needed by DB queue workers.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/queue/PostgresRunQueue.java` - PostgreSQL/JdbcTemplate implementation of `RunQueue`.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/InMemoryCancellationRegistry.java` - Concurrent in-memory cancellation token registry.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` - Concrete run worker/dispatcher for claimed queued runs.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/RunWorkerScheduler.java` - Plain worker scheduler wrapper around `RunDispatcher`.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/queue/PostgresRunQueueTest.java` - Testcontainers PostgreSQL queue semantics tests.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/InMemoryCancellationRegistryTest.java` - Cancellation token lifecycle tests.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcherTest.java` - Success/cancel/timeout/failure dispatcher orchestration tests.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/execution/RunWorkerSchedulerTest.java` - Poll and async trigger scheduler tests.

## Decisions Made

- Kept all new execution classes as plain Infrastructure classes with constructor dependencies; active Spring composition is intentionally deferred to Plan 02-12.
- Stored full queued-run context in `run_queue` because `QueuedRun` is required for cancellation terminal event publication and runtime `RunContext` construction.
- Used dispatcher-side timeout enforcement around `AgentRuntime.start(context)` so a timed-out worker can call `AgentRuntime.cancel(runId, "timeout")`, publish a timeout terminal fallback, and mark queue/projection state consistently.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical Functionality] Added queued-run payload/context columns to `run_queue` migration**
- **Found during:** Task 1 (PostgreSQL queue implementation)
- **Issue:** The existing `run_queue` table only contained `run_id`, status, lease, and timestamps. The planned `PostgresRunQueue` must return a full `QueuedRun` from claim/cancel operations and dispatcher must build `RunContext`; without storing session/tenant/user/workspace/trace/correlation/input columns, the queue implementation could not satisfy the App port correctly.
- **Fix:** Extended `V1__create_cloud_runtime_tables.sql` with queued-run context fields, `input_type`, `input jsonb`, and `cancel_reason`.
- **Files modified:** `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test-compile -pl pi-agent-infrastructure -am`; PostgreSQL runtime tests are written but require Docker.
- **Committed in:** `6494af5`

---

**Total deviations:** 1 auto-fixed (1 missing critical functionality)
**Impact on plan:** Required for correctness of the planned queue and dispatcher behavior; no scope creep beyond DB queue contract support.

## Issues Encountered

- Docker is unavailable in this execution environment (`/var/run/docker.sock` missing), so Testcontainers-backed `PostgresRunQueueTest` could not execute locally. The test class compiles and should be run on a Docker-enabled host/CI runner.

## Verification

- Passed: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest='InMemoryCancellationRegistryTest,DefaultRunDispatcherTest,RunWorkerSchedulerTest'`
- Passed: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test-compile -pl pi-agent-infrastructure -am`
- Docker-gated: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest='PostgresRunQueueTest,InMemoryCancellationRegistryTest,DefaultRunDispatcherTest,RunWorkerSchedulerTest'` failed before queue assertions because Testcontainers could not find a Docker environment.

## Known Stubs

None. Fallback strings in `DefaultRunDispatcher.runInput` are defensive normalization for blank/missing queued input values, not UI/rendering stubs.

## User Setup Required

- Docker is required to execute `PostgresRunQueueTest` because it uses Testcontainers PostgreSQL for real JSONB and `FOR UPDATE SKIP LOCKED` semantics.

## Next Phase Readiness

- Plan 02-12 can wire `PostgresRunQueue`, `InMemoryCancellationRegistry`, `DefaultRunDispatcher`, and `RunWorkerScheduler` as the single active Spring composition root.
- Plan 02-13/CI should run the Docker-gated PostgreSQL queue tests on a Docker-enabled runner.

## Self-Check: PASSED

- Created/modified files listed above exist in the repository.
- Task commits verified in git history: `6494af5`, `3aa6bd7`.
- No active Spring bean/configuration ownership was added by this plan.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
