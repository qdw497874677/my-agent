---
phase: 02-cloud-server-persistence-sse-and-baseline-security
plan: 06
subsystem: app-usecases
tags: [java, cola, app-layer, cancellation, run-events, audit, queue]

requires:
  - phase: 02-cloud-server-persistence-sse-and-baseline-security
    provides: App request context, use-case interfaces, persistence ports, audit ports, queue ports, and cancellation ports from plans 02-04 and 02-05
provides:
  - Framework-free concrete App use-case services for sessions, run queries, run create, and run cancellation
  - Idempotent terminal event publisher guarded by durable RunEventStore terminal checks
  - Unit and architecture coverage for App orchestration and Spring/JDBC/Servlet/SSE-free implementations
affects: [phase-02-infrastructure, phase-02-web-adapter, phase-02-sse, phase-02-e2e]

tech-stack:
  added: []
  patterns: [constructor-injected plain Java App services, durable cancellation orchestration, App-local event DTO mapping, terminal event guard]

key-files:
  created:
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultSessionCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultSessionQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java
    - pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunTerminalEventPublisher.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultSessionUseCaseTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryServiceTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandServiceTest.java
    - pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/AppUseCaseImplementationArchTest.java
  modified: []

key-decisions:
  - "Keep concrete App services as plain Java classes with constructor injection and no Spring/JDBC/Servlet/SSE imports."
  - "Route App-created terminal run events through DefaultRunTerminalEventPublisher so RunEventStore.hasTerminalEvent guards duplicate terminal publication."
  - "Use queued-run context to build queued cancellation terminal events, preserving tenant/user/session/run/workspace/trace/correlation IDs."

patterns-established:
  - "App command services orchestrate ports only: repository, audit, queue, cancellation registry, and terminal publisher."
  - "Run query service delegates read-model queries while mapping domain RunEvent envelopes into client RunEventDto history responses."
  - "Queued cancellation performs durable cancel request, queue cancellation, terminal state transition, queue terminal marking, and idempotent terminal event publication."

requirements-completed: [CLOUD-01, CLOUD-03, CLOUD-04, CLOUD-05, CLOUD-06, E2E-04]

duration: 7m 14s
completed: 2026-06-14
---

# Phase 02 Plan 06: Concrete App Use Cases and Cancellation Summary

**Framework-free App orchestration for session/run commands, run queries, durable cancellation, audit, queue handoff, and idempotent terminal events**

## Performance

- **Duration:** 7m 14s
- **Started:** 2026-06-14T05:21:55Z
- **Completed:** 2026-06-14T05:29:09Z
- **Tasks:** 2
- **Files modified:** 9

## Accomplishments

- Implemented `DefaultSessionCommandService`, `DefaultSessionQueryService`, and `DefaultRunQueryService` as plain Java App services over existing App ports.
- Implemented `DefaultRunCommandService` for create-run and idempotent cancellation orchestration across projection repository, run queue, cancellation registry, terminal publisher, and audit repository.
- Implemented `DefaultRunTerminalEventPublisher` with `RunEventStore.hasTerminalEvent` guards and terminal `run.completed`, `run.cancelled`, and `run.failed` publication through the domain `EventSink`.
- Added unit coverage for session creation/query, run query delegation/event cursor computation, run create/cancel paths, and terminal event duplicate suppression.
- Added an App implementation architecture test preventing `Default*Service.java` from importing Spring, JDBC, Servlet, or SSE framework types.

## Task Commits

Each task was committed atomically:

1. **Task 1: Implement session and query use-case services** - `341ed43` (feat)
2. **Task 2: Implement run create and idempotent cancellation orchestration** - `692be17` (feat)

**Plan metadata:** `4f57a9e` (initial docs/state/roadmap commit)

## Files Created/Modified

- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultSessionCommandService.java` - Creates sessions through `SessionRepository` and records `session.create` audit actions.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultSessionQueryService.java` - Delegates session lookup/history and throws `NoSuchElementException` when a session is missing.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryService.java` - Delegates run read-model queries and maps persisted `RunEvent` values into `EventHistoryResponse` with cursor and terminal state.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java` - Orchestrates run creation, queue enqueue, audit, durable cancellation request, queued cancellation terminal transition, active runtime cancellation signal, and terminal no-op behavior.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunTerminalEventPublisher.java` - Publishes terminal events only when no durable terminal event exists.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultSessionUseCaseTest.java` - Covers session create/query use-case behavior.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultRunQueryServiceTest.java` - Covers event history query delegation and run query repository delegation.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandServiceTest.java` - Covers create run, queued cancellation, running cancellation, terminal cancellation no-op, and terminal publisher duplicate guard.
- `pi-agent-app/src/test/java/io/github/pi_java/agent/app/usecase/AppUseCaseImplementationArchTest.java` - Enforces framework-free App default service implementations.

## Decisions Made

- Kept concrete App use-case services Spring-free and JDBC-free to preserve COLA boundaries and future embeddability.
- Centralized App-created terminal run events behind `DefaultRunTerminalEventPublisher` rather than creating terminal events directly in `DefaultRunCommandService`.
- Used `RunEventPayload.ExtensionPayload` for App-created terminal event payload details so timeout can use the allowed `run.failed` wire name with explicit `TIMED_OUT` status metadata.

## Deviations from Plan

None - plan executed exactly as written.

## Known Stubs

None. Stub scan only found null-handling branches for optional failure summaries/reasons and did not identify placeholder UI/data-source stubs.

## Issues Encountered

- TDD RED initially exposed two expected missing implementation classes and a test fixture type mismatch for `SecurityPrincipalContext` authorities. The fixture mismatch was corrected before GREEN implementation; no production deviation was required.
- Existing uncommitted/untracked files from parallel Phase 2 agents were present before execution and intentionally left untouched.

## Verification

- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest='DefaultSessionUseCaseTest,DefaultRunQueryServiceTest'`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest='DefaultRunCommandServiceTest,AppUseCaseImplementationArchTest,AppDependencyArchTest'`
- `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-app -am -Dtest='DefaultSessionUseCaseTest,DefaultRunCommandServiceTest,DefaultRunQueryServiceTest,AppUseCaseImplementationArchTest,AppDependencyArchTest'`

## Self-Check: PASSED

- Found summary file at `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-06-SUMMARY.md`.
- Found created implementation files including `DefaultRunCommandService.java` and `DefaultRunTerminalEventPublisher.java`.
- Verified task commits exist: `341ed43` and `692be17`.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Plan 02-07 can implement PostgreSQL/Flyway/JDBC persistence behind the concrete App services and existing App ports.
- Later REST/SSE adapters can call these use cases without embedding persistence, queue, cancellation, or audit orchestration in controllers.
- Queued cancellation now has a single App-level path for durable terminal state and event publication, ready for DB queue and event sink implementation.

---
*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Completed: 2026-06-14*
