---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 07
subsystem: database
tags: [postgresql, flyway, jdbc, testcontainers, sse, event-store]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App persistence ports and terminal event publishing contracts from plans 02-05/02-06
provides:
  - PostgreSQL Flyway schema for sessions, runs, run_events, projections, audit_records, and run_queue
  - JDBC implementations for run event append/replay, run projections, sessions, and audit records
  - Persist-then-emit EventSink that commits durable state before live fanout
affects: [adapter-web, sse, run-worker, cloud-e2e, persistence]

tech-stack:
  added: [PostgreSQL JSONB schema, Spring JdbcTemplate repositories, TransactionTemplate event sink, JUnit/Testcontainers persistence test]
  patterns: [append-only event store, idempotent terminal projection, persist-then-emit fanout]

key-files:
  created:
    - pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcAuditRepository.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/RunEventFanout.java
    - pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java
    - pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcPersistenceIntegrationTest.java
  modified:
    - pi-agent-infrastructure/pom.xml
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java

key-decisions:
  - "Use PostgreSQL JSONB plus explicit payload_schema/payload_version columns for durable RunEvent storage without Java class-name serialization."
  - "Expose updateLastEventSequence as a default App-port method so Infrastructure EventSink can project event progress without breaking existing App tests."

patterns-established:
  - "Persist-then-emit: EventSink appends the event and updates projections in a TransactionTemplate before calling RunEventFanout.publish."
  - "Terminal idempotency: run projection updates guard with status NOT IN terminal statuses."

requirements-completed: [CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-06, E2E-05]

duration: 9m 55s
completed: 2026-06-14
---

# Phase 02 Plan 07: Flyway/JDBC Persistence and Persist-Then-Emit Event Sink Summary

**PostgreSQL event-store persistence with ordered replay, idempotent terminal projections, audit context, and DB-before-SSE fanout semantics**

## Performance

- **Duration:** 9m 55s
- **Started:** 2026-06-14T05:21:55Z
- **Completed:** 2026-06-14T05:31:50Z
- **Tasks:** 2
- **Files modified:** 10

## Accomplishments

- Added Flyway `V1__create_cloud_runtime_tables.sql` covering `sessions`, `session_entries`, `runs`, `run_events`, `steps`, `messages`, `tool_calls`, `audit_records`, and `run_queue`, including per-run event sequence uniqueness and required query indexes.
- Implemented Infrastructure JDBC repositories for App persistence ports: ordered event append/replay, terminal detection, session create/history lookup, run projection mutation/query, and audit record insertion with trace/correlation context.
- Added `RunEventFanout` plus `PersistingEventSink`, preserving the Phase 2 contract that durable event/projection writes complete before live subscribers observe events.
- Added Testcontainers PostgreSQL integration coverage for schema creation, per-run sequence uniqueness, event replay ordering, terminal idempotency, audit context, append failure fanout suppression, and persist-before-fanout behavior.

## Task Commits

Each task was committed atomically:

1. **Task 1: Create Flyway schema and JDBC repositories** - `a9f4f6d` (feat)
2. **Task 2: Implement persist-then-emit EventSink** - `df46868` (feat)

## Files Created/Modified

- `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` - PostgreSQL durable runtime schema, constraints, and indexes.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcJson.java` - Infrastructure-local JSONB serialization helper.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunEventStore.java` - Append-only run event store with ordered replay and terminal event detection.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcRunProjectionRepository.java` - Run read-model persistence with running/cancellation/terminal status updates and query helpers.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcSessionRepository.java` - Session create/find/history repository.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcAuditRepository.java` - Audit record repository preserving tenant/user/trace/correlation context.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/RunEventFanout.java` - Live event fanout seam for later SSE adapter implementation.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java` - Domain `EventSink` implementation that persists and projects before fanout.
- `pi-agent-infrastructure/src/test/java/io/github/pi_java/agent/infrastructure/jdbc/JdbcPersistenceIntegrationTest.java` - PostgreSQL integration test contract for persistence and fanout ordering.
- `pi-agent-infrastructure/pom.xml` - Added explicit JUnit Jupiter and AssertJ test dependencies for Infrastructure tests.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/RunProjectionRepository.java` - Added default `updateLastEventSequence` projection hook.

## Decisions Made

- Use explicit `payload_schema` and `payload_version` storage with JSONB payload bodies, avoiding `@class` or provider/JVM class-name serialization leakage.
- Keep the fanout seam in Infrastructure (`RunEventFanout`) while implementing the Domain `EventSink` port there, preserving COLA direction and keeping Domain framework-free.
- Add `updateLastEventSequence` as a default method on the App port to avoid disrupting existing App implementations while enabling durable projection progress.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Added missing Infrastructure test dependencies**
- **Found during:** Task 1 (Create Flyway schema and JDBC repositories)
- **Issue:** `pi-agent-infrastructure` declared Testcontainers but not direct JUnit Jupiter/AssertJ dependencies, so `JdbcPersistenceIntegrationTest` test compilation could not resolve test APIs.
- **Fix:** Added explicit `org.junit.jupiter:junit-jupiter` and `org.assertj:assertj-core` test dependencies to the Infrastructure module POM.
- **Files modified:** `pi-agent-infrastructure/pom.xml`
- **Verification:** Infrastructure compile reached the Testcontainers execution gate after dependency fix.
- **Committed in:** `a9f4f6d`

**Total deviations:** 1 auto-fixed (Rule 3 blocking)
**Impact on plan:** Required for the planned Testcontainers integration test to compile; no functional scope creep.

## Issues Encountered

- The planned Testcontainers verification command reaches the Docker environment gate in this container: `/var/run/docker.sock` is unavailable. This matches the plan's explicit Docker caveat. A compile-only reactor check was green before the container startup failure, and the targeted test should be run on a Docker-enabled host/CI runner.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -am -DskipTests compile` — passed.
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure -Dtest=JdbcPersistenceIntegrationTest test` — blocked by missing Docker/Testcontainers runtime (`Could not find a valid Docker environment`).

## Known Stubs

- `JdbcRunProjectionRepository` list/detail query methods currently return direct read-model rows and empty event list in `getRunDetail`; event DTO mapping is intentionally deferred to later adapter/query plans. This does not block Plan 07's persistence/event-store goal.
- `JdbcRunEventStore` reconstructs persisted rows as `RunEventPayload.ExtensionPayload` for replay until adapter-level event DTO mapping is introduced; payload schema/version/body are durably preserved.

## User Setup Required

Run the planned PostgreSQL integration gate on a Docker-enabled host or CI runner:

```text
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest=JdbcPersistenceIntegrationTest
```

## Next Phase Readiness

- Plan 08 can build DB-backed queue/cancellation behavior on the existing `run_queue` table and JDBC/transaction patterns.
- Plan 11 can implement SSE fanout by adapting `RunEventFanout` and relying on the DB-before-live-event guarantee.
- Plan 12 wiring can register `PersistingEventSink` as the runtime `EventSink` bean and route `RunEventFanout` to the adapter-web SSE implementation.

## Self-Check: PASSED

- Found created files: migration, JDBC repositories, event fanout seam, persisting event sink, integration test.
- Found task commits: `a9f4f6d`, `df46868`.
- Verified no plan-goal-blocking stubs remain; intentional deferred adapter/query mapping stubs are documented above.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
