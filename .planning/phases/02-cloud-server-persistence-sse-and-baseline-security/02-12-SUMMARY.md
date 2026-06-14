---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 12
subsystem: cloud-runtime-wiring
tags: [spring-boot, jdbc, sse, worker, runtime-composition, cola]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App use cases, JDBC persistence, DB queue, worker dispatcher, REST controllers, and SSE fanout implementation
provides:
  - Single Spring composition root for Phase 2 runtime wiring
  - Scheduler-backed REST run activation path through RunWorkerScheduler.triggerAsync()
  - Bean uniqueness tests for critical runtime infrastructure
affects: [phase-02-e2e, cloud-server, persistence, sse, worker-runtime]

tech-stack:
  added: []
  patterns: [single Spring composition root, persist-before-fanout event sink, conditional fallback activation trigger]

key-files:
  created:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java
    - pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudRuntimeWiringIntegrationTest.java
  modified:
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java
    - pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java

key-decisions:
  - "Phase 02 Plan 12 owns active runtime bean registration through CloudRuntimeBeanConfiguration instead of component scanning infrastructure fanout beans."
  - "RunController activation remains abstracted behind RunActivationTrigger, with production wiring delegating to RunWorkerScheduler.triggerAsync()."
  - "Scheduled polling is provided by a small adapter bean so RunWorkerScheduler remains a plain infrastructure class and exactly one scheduler bean exists."

patterns-established:
  - "Composition root pattern: Adapter Web config assembles App services, Infrastructure implementations, Domain AgentRuntime, EventSink, and SSE fanout at the outer layer."
  - "Persist-before-SSE chain: PersistingEventSink receives TransactionTemplate, RunEventStore, RunProjectionRepository, and SseRunEventFanout through one bean path."

requirements-completed: [CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-01, E2E-04, E2E-05]

duration: 7m 01s
completed: 2026-06-14
---

# Phase 02 Plan 12: Cloud Runtime Composition Root Summary

**Single Spring runtime composition root wiring App services, JDBC persistence, DB queue, dispatcher, worker scheduler, persist-before-SSE EventSink, and scheduler-backed run activation**

## Performance

- **Duration:** 7m 01s
- **Started:** 2026-06-14T06:02:24Z
- **Completed:** 2026-06-14T06:09:25Z
- **Tasks:** 1
- **Files modified:** 4

## Accomplishments

- Added `CloudRuntimeBeanConfiguration` as the single active Spring composition root for runtime-facing App, persistence, queue, worker, EventSink, and SSE fanout beans.
- Wired `PersistingEventSink(TransactionTemplate, RunEventStore, RunProjectionRepository, RunEventFanout)` so events persist/update projections before live SSE fanout publication.
- Wired `RunActivationTrigger` to `RunWorkerScheduler.triggerAsync()` and kept `RunController` decoupled from `DefaultRunDispatcher`.
- Added `CloudRuntimeWiringIntegrationTest` covering required method names, critical bean uniqueness, product-path wiring, and scheduler-backed activation.

## Task Commits

Each task was committed atomically:

1. **Task 1: Wire all runtime beans in one composition root** - `71235c4` (feat)

**Plan metadata:** pending final docs commit

## Files Created/Modified

- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` - Defines the Phase 2 composition root for `SessionCommandService`, `SessionQueryService`, `RunCommandService`, `RunQueryService`, JDBC repositories, `PostgresRunQueue`, `InMemoryCancellationRegistry`, `DefaultRunTerminalEventPublisher`, `DefaultRunDispatcher`, `RunWorkerScheduler`, `SseRunEventFanout`, `PersistingEventSink`, and scheduler polling/activation adapter beans.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudRuntimeWiringIntegrationTest.java` - Verifies critical bean uniqueness and the controller-to-App-to-JDBC-dispatcher-runtime-EventSink-SSE wiring chain.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java` - Converted the fallback no-op trigger to `@ConditionalOnMissingBean` so production wiring owns active activation when present.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java` - Removed component scanning registration so the composition root owns the single `RunEventFanout` bean.

## Decisions Made

- Phase 02 Plan 12 owns active runtime bean registration through `CloudRuntimeBeanConfiguration` instead of component scanning infrastructure fanout beans.
- `RunController` activation remains abstracted behind `RunActivationTrigger`, with production wiring delegating to `RunWorkerScheduler.triggerAsync()`.
- Scheduled polling is provided by a small adapter bean so `RunWorkerScheduler` remains a plain infrastructure class and exactly one scheduler bean exists.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Avoided duplicate SSE fanout bean registration**
- **Found during:** Task 1 (Wire all runtime beans in one composition root)
- **Issue:** `SseRunEventFanout` was annotated with `@Component`, which created a second active `RunEventFanout` bean once the composition root also registered it.
- **Fix:** Removed `@Component` and made the composition root the owner of the single `SseRunEventFanout` / `RunEventFanout` bean.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/SseRunEventFanout.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudRuntimeWiringIntegrationTest`
- **Committed in:** `71235c4`

**2. [Rule 3 - Blocking] Made the legacy no-op activation trigger conditional**
- **Found during:** Task 1 (Wire all runtime beans in one composition root)
- **Issue:** Existing `NoopRunActivationTrigger` would conflict with the production `RunActivationTrigger` required to call `RunWorkerScheduler.triggerAsync()`.
- **Fix:** Added `@ConditionalOnMissingBean(RunController.RunActivationTrigger.class)` so the no-op remains only as a fallback for controller-only tests.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudRuntimeWiringIntegrationTest`
- **Committed in:** `71235c4`

**3. [Rule 3 - Blocking] Qualified the worker executor dependency**
- **Found during:** Task 1 (Wire all runtime beans in one composition root)
- **Issue:** Spring scheduling also exposes an `Executor`, so `RunWorkerScheduler` injection was ambiguous between `runWorkerExecutor` and `taskScheduler`.
- **Fix:** Qualified the `RunWorkerScheduler` factory method parameter with `@Qualifier("runWorkerExecutor")`.
- **Files modified:** `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java`
- **Verification:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudRuntimeWiringIntegrationTest`
- **Committed in:** `71235c4`

---

**Total deviations:** 3 auto-fixed (3 blocking)
**Impact on plan:** All fixes were necessary to make the single-composition-root invariant true. No scope creep.

## Issues Encountered

- The first test approach used Testcontainers for PostgreSQL, but this execution environment has no Docker socket. The wiring test was changed to validate bean composition with mocked `JdbcTemplate` and `TransactionTemplate` instead, while still using real composition-root bean definitions.
- A module-wide reactor test run is currently blocked by pre-existing Docker-dependent Testcontainers tests (`JdbcPersistenceIntegrationTest`, `PostgresRunQueueTest`) in upstream modules. The plan-specific wiring verification passes.

## Known Stubs

None - no stubs or placeholder data sources were introduced for the plan goal.

## User Setup Required

None - no external service configuration required for this plan beyond Docker being needed for unrelated Testcontainers suites.

## Verification

- Passed: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudRuntimeWiringIntegrationTest`
- Blocked by environment: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am` fails in existing Testcontainers suites because `/var/run/docker.sock` is unavailable.

## Next Phase Readiness

- Plan 02-13 can build E2E/API-doc verification on top of a single, explicit Cloud Runtime composition root.
- REST-created runs now have a production activation path through `RunActivationTrigger -> RunWorkerScheduler.triggerAsync()` rather than controller-to-dispatcher coupling.

## Self-Check: PASSED

- Found created file: `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java`
- Found created file: `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudRuntimeWiringIntegrationTest.java`
- Found task commit: `71235c4`

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
