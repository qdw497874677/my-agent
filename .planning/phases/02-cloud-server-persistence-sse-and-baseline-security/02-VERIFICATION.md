---
phase: 02-cloud-server-persistence-sse-and-baseline-security
verified: 2026-06-14T06:28:00Z
status: human_needed
score: "5/6 must-haves verified locally; 1/6 Docker/Testcontainers-gated"
human_verification:
  - test: "Run full Docker-enabled Phase 2 Testcontainers suite"
    expected: "`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test` passes on a host with `/var/run/docker.sock` or equivalent Docker-compatible runtime."
    why_human: "This environment has no Docker socket, so PostgreSQL/Testcontainers E2E and JDBC queue/persistence gates cannot start."
---

# Phase 2: Cloud Server, Persistence, SSE, and Baseline Security Verification Report

**Phase Goal:** Prove Pi is a Cloud Server product by exposing runtime through REST/SSE and durable PostgreSQL-backed state.
**Verified:** 2026-06-14T06:28:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

Phase 2 is implemented and locally verified for the non-Docker gate. The only unverified portion in this environment is the Docker/Testcontainers PostgreSQL suite required to prove Flyway/JDBC/queue/headless E2E against a real PostgreSQL container.

### Observable Truths

| # | Truth | Status | Evidence |
| --- | --- | --- | --- |
| 1 | Authenticated REST API can create a run, fetch run detail, fetch status, list events, list steps/messages/tool calls, and cancel a run. | ✓ VERIFIED | `RunController.java` exposes `POST /api/sessions/{sessionId}/runs`, `GET /{runId}`, `/status`, `/events`, `/steps`, `/messages`, `/tool-calls`, `/result`, and `POST /cancel`, all delegating to App use cases with `RequestContext`. `RunApiIntegrationTest` and `RunQueryIntegrationTest` passed in the non-Docker targeted suite. |
| 2 | SSE stream emits the same provider-neutral RunEvent envelope stored in persistence. | ✓ VERIFIED | `RunEventDto` is the public event envelope; `DefaultRunQueryService` maps `RunEventStore` rows to `RunEventDto`; `SseRunEventFanout.publish` maps `RunEvent` to `RunEventDto`; `RunEventStreamService` sends `RunEventDto` with sequence SSE id and event type. `RunSseIntegrationTest` passed locally. |
| 3 | PostgreSQL Flyway migrations create durable tables for sessions, runs, steps, messages, tool calls, events, and audit basics. | ? DOCKER-GATED | `V1__create_cloud_runtime_tables.sql` defines `sessions`, `session_entries`, `runs`, `run_events`, `steps`, `messages`, `tool_calls`, `audit_records`, and `run_queue` with indexes and sequence uniqueness. Actual Flyway/JDBC integration test execution is blocked by missing Docker. |
| 4 | Cancellation through REST changes run state and appears in event history. | ✓ VERIFIED | `DefaultRunCommandService.cancelRun` updates durable cancellation state, records audit, handles queued cancellation via `cancelQueuedAndReturn`, signals active cancellation via `CancellationRegistry`, and publishes `run.cancelled` through `RunTerminalEventPublisher` when appropriate. Non-Docker dispatcher/cancellation tests passed; full DB-backed cancellation E2E is Docker-gated. |
| 5 | Health endpoints, structured logs, request correlation IDs, and tenant/user placeholder context are present. | ✓ VERIFIED | `SecurityConfig` permits `/actuator/health` and `/actuator/info`, authenticates `/api/**`, and configures JWT-ready resource server. `DevAuthenticationFilter` maps dev/test headers to `PiPrincipal`; `CorrelationFilter` sets `X-Correlation-ID`, request attributes, and MDC keys. `SecurityAndCorrelationIntegrationTest` passed locally. |
| 6 | Headless E2E can create a run, stream events, persist state, query history, cancel a run, and verify SSE ordering/reconnect behavior without real model keys. | ? DOCKER-GATED | `CloudServerHeadlessE2ETest`, `RunCancellationIntegrationTest`, and `TestCloudRuntimeConfiguration` exist with fake `GeneralAgentLoop`/model/tool/policy wiring and `PostgreSQLContainer<>("postgres:17-alpine")`. Cannot execute in this environment because Docker is unavailable. |

**Score:** 5/6 locally verified; 1/6 requires Docker-enabled verification.

### Required Artifacts

| Artifact | Expected | Status | Details |
| --- | --- | --- | --- |
| `pom.xml` | Spring Boot, Java 21, Testcontainers foundation | ✓ VERIFIED with warning | Contains Java 21 compiler release, Spring Boot `3.5.9`, `testcontainers.version` property `2.0.3`, and Boot/Testcontainers dependency management. Warning: effective dependency tree resolves Testcontainers `1.21.4`, not `2.0.3`. |
| `pi-agent-client/src/main/java/io/github/pi_java/agent/client/run/CreateRunRequest.java` | Generic create-run DTO, not chat-only | ✓ VERIFIED | Public DTO includes `inputType` and generic `input`; client layer does not import Domain types. |
| `pi-agent-client/src/main/java/io/github/pi_java/agent/client/event/RunEventDto.java` | Provider-neutral event envelope | ✓ VERIFIED | Event DTO has sequence, trace/correlation IDs, visibility, redaction, payload schema/version, and payload. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/context/RequestContext.java` | Tenant/user/trace/correlation context | ✓ VERIFIED | App use cases accept `RequestContext`; App layer remains Spring/JDBC/Servlet-free by grep and architecture test coverage. |
| `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultRunCommandService.java` | Create/cancel orchestration | ✓ VERIFIED | Wires projection repository, queue, cancellation registry, audit, and terminal-event publisher. |
| `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` | Flyway runtime schema | ✓ STRUCTURALLY VERIFIED / DOCKER-GATED | Defines required tables and indexes. Runtime Flyway execution requires Docker/Testcontainers. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java` | Persist-then-emit event sink | ✓ VERIFIED | `eventStore.append` and projection updates occur inside `tx.executeWithoutResult`; `fanout.publish(event)` occurs after transaction block. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/queue/PostgresRunQueue.java` | DB-backed queue with SKIP LOCKED and queued cancellation | ✓ STRUCTURALLY VERIFIED / DOCKER-GATED | Contains `FOR UPDATE SKIP LOCKED`, `cancelQueuedAndReturn`, lease fields, and terminal handling. PostgreSQL behavior requires Docker-enabled tests. |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/execution/DefaultRunDispatcher.java` | Runtime dispatcher/worker | ✓ VERIFIED | Claims runs, marks running, constructs `RunContext`, calls `AgentRuntime.start`, handles completion/cancel/timeout/failure, removes cancellation tokens. `DefaultRunDispatcherTest` passed locally. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/security/SecurityConfig.java` | Security baseline | ✓ VERIFIED | Health/info public, `/api/**` authenticated, dev/test filter, OAuth2 resource-server shape. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunController.java` | Session-centric REST API | ✓ VERIFIED | Endpoints are present, return client DTOs, delegate to App use cases, and invoke `RunActivationTrigger` rather than dispatcher internals. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/sse/RunEventStreamService.java` | Replay-before-subscribe SSE | ✓ VERIFIED | Calls `listEvents` before `fanout.subscribe`, sends SSE id as run sequence, supports `Last-Event-ID`, and cleanup hooks. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` | Single active composition root | ✓ VERIFIED | Defines beans for App services, JDBC repositories, queue, cancellation registry, dispatcher, scheduler, `EventSink`, and `SseRunEventFanout`; `CloudRuntimeWiringIntegrationTest` passed. |
| `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/CloudServerHeadlessE2ETest.java` | No-key REST/SSE/persistence E2E | ? DOCKER-GATED | Test exists and uses `PostgreSQLContainer<>("postgres:17-alpine")`; execution blocked by missing Docker. |
| `docs/phase-02-cloud-server-api.md` | Downstream API contract index | ✓ VERIFIED | Documents all Phase 2 endpoints, event DTO fields, SSE replay contract, persistence/worker path, and explicit non-goals. |

### Key Link Verification

| From | To | Via | Status | Details |
| --- | --- | --- | --- | --- |
| REST controllers | App use cases | `SessionController.toRequestContext(principal, servletRequest)` and use-case interface calls | ✓ WIRED | `RunController` delegates create/cancel/query operations to `RunCommandService`/`RunQueryService`; `SessionController` similarly delegates sessions. |
| Create-run REST path | Worker activation | `RunActivationTrigger` bean delegates to `RunWorkerScheduler.triggerAsync()` | ✓ WIRED | `RunController.createRun` calls `runActivationTrigger.triggerAsync()`; `CloudRuntimeBeanConfiguration.runActivationTrigger` returns `runWorkerScheduler::triggerAsync`; wiring test verifies this. |
| App create-run | Durable run row, audit, queue | `DefaultRunCommandService.createRun` | ✓ WIRED | Calls `runProjectionRepository.createRun`, `runQueue.enqueue`, `auditRepository.record`, then returns persisted `RunResponse`. |
| REST cancel | Durable cancel, queue cancel, active token, terminal event | `DefaultRunCommandService.cancelRun` | ✓ WIRED | Calls `requestCancellation`, `cancelQueuedAndReturn`, `publishCancelledIfAbsent` for queued runs, or `cancellationRegistry.requestCancellation` for active runs. |
| DB queue | Runtime dispatcher | `RunWorkerScheduler.pollOnce/triggerAsync` to `RunDispatcher.dispatch` | ✓ WIRED | Scheduler calls dispatcher; dispatcher claims queue rows and calls `AgentRuntime.start(context)`. |
| Runtime events | Persistence then SSE | `PersistingEventSink` → `RunEventStore`/projection → `RunEventFanout` | ✓ WIRED | Code persists and updates projection inside transaction, then calls fanout after transaction returns. |
| Event history | SSE envelope | Shared `RunEventDto` | ✓ WIRED | REST history and SSE both send `RunEventDto`, preserving provider-neutral envelope fields. |
| SSE reconnect | Durable replay | `afterSequence`/`Last-Event-ID` → `RunQueryService.listEvents` | ✓ WIRED | `RunEventStreamService` resolves `Last-Event-ID`, replays events with `sequence > cursor`, then subscribes to live fanout. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
| --- | --- | --- | --- | --- |
| `RunController` | `RunResponse`, `RunStatusResponse`, `EventHistoryResponse`, page DTOs | App use cases backed by JDBC repositories in composition root | Yes, through repository/queue/event-store implementations | ✓ FLOWING |
| `DefaultRunQueryService` | `events` | `RunEventStore.listByRun(runId, afterSequence, limit)` | Yes, JDBC `run_events` query ordered by sequence | ✓ FLOWING |
| `RunEventStreamService` | replay/live `RunEventDto` | `RunQueryService.listEvents` plus `SseRunEventFanout.subscribe` | Yes, persisted replay plus live fanout after persistence | ✓ FLOWING |
| `PersistingEventSink` | `RunEvent` | Runtime/terminal publisher | Yes, persists event rows and updates run projection before fanout | ✓ FLOWING |
| `CloudServerHeadlessE2ETest` | E2E run lifecycle assertions | Testcontainers PostgreSQL + fake runtime | Expected yes; not executable locally without Docker | ? DOCKER-GATED |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
| --- | --- | --- | --- |
| Non-Docker Phase 2 integration/unit target passes with Java 21 | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='CloudRuntimeWiringIntegrationTest,RunSseIntegrationTest,RunApiIntegrationTest,RunQueryIntegrationTest,SecurityAndCorrelationIntegrationTest,DefaultRunDispatcherTest,InMemoryCancellationRegistryTest,RunWorkerSchedulerTest'` | Completed successfully. | ✓ PASS |
| Full Java 21 suite reaches Docker/Testcontainers gate | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test` | Fails in Testcontainers startup because no valid Docker environment: missing `/var/run/docker.sock`; observed failing classes `JdbcPersistenceIntegrationTest` and `PostgresRunQueueTest` before later Docker E2E classes could run. | ? DOCKER-GATED |
| Effective Testcontainers dependency version | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -pl pi-agent-infrastructure dependency:tree -Dincludes=org.testcontainers` | Resolved `org.testcontainers:*:1.21.4:test`, despite root property `testcontainers.version=2.0.3`. | ⚠️ WARNING |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
| --- | --- | --- | --- | --- |
| CLOUD-01 | 02-01, 02-02, 02-04, 02-05, 02-06, 02-08, 02-09, 02-10, 02-12, 02-13 | User can create an Agent Run through authenticated REST API. | ✓ SATISFIED | Security requires auth for `/api/**`; `RunController.createRun` delegates to `RunCommandService.createRun`, enqueues run, and triggers scheduler activation. |
| CLOUD-02 | 02-01, 02-03, 02-05, 02-07, 02-11, 02-12, 02-13 | User can stream RunEvents through SSE using persisted envelope. | ✓ SATISFIED / Docker replay E2E gated | `RunEventStreamController` and `RunEventStreamService` expose SSE and replay persisted `RunEventDto`; live fanout receives `RunEvent` after `PersistingEventSink` persistence. |
| CLOUD-03 | 02-01, 02-02, 02-04, 02-05, 02-06, 02-07, 02-10, 02-11, 02-12, 02-13 | User can query status, detail, events, steps, messages, tool calls, and result. | ✓ SATISFIED | `RunController` exposes all query endpoints; repository/query implementations exist; non-Docker controller/query tests pass. |
| CLOUD-04 | 02-01, 02-02, 02-04, 02-05, 02-06, 02-07, 02-08, 02-10, 02-12, 02-13 | User can cancel running run and observe cancellation in state and event stream. | ✓ SATISFIED / DB-backed E2E gated | App cancellation orchestration and dispatcher cancellation paths exist; Docker E2E confirms against PostgreSQL when available. |
| CLOUD-05 | 02-01, 02-02, 02-03, 02-04, 02-06, 02-09, 02-10, 02-11, 02-13 | Baseline auth/security context, tenant/user placeholders, correlation IDs, structured logs, health endpoints. | ✓ SATISFIED | `SecurityConfig`, `DevAuthenticationFilter`, `CorrelationFilter`, Actuator config, and passing `SecurityAndCorrelationIntegrationTest`. |
| CLOUD-06 | 02-01, 02-05, 02-06, 02-07, 02-08, 02-12, 02-13 | Durable PostgreSQL-backed state with migrations. | ? DOCKER-GATED | SQL schema and JDBC repositories are present; Testcontainers PostgreSQL execution blocked by missing Docker. |
| E2E-01 | 02-01, 02-02, 02-08, 02-12, 02-13 | Headless E2E creates run, streams events, persists state, terminal status without model keys. | ? DOCKER-GATED | E2E test and fake runtime exist; cannot execute here without Docker. |
| E2E-04 | 02-01, 02-05, 02-06, 02-08, 02-12, 02-13 | Headless E2E verifies cancellation, timeout, max-step, terminal events, no hanging tasks. | ? DOCKER-GATED | `RunCancellationIntegrationTest` and E2E max-step/timeout assertions exist; require Docker. |
| E2E-05 | 02-01, 02-03, 02-07, 02-11, 02-12, 02-13 | Headless E2E verifies SSE ordering, terminal events, reconnect/replay. | ? DOCKER-GATED | `RunSseIntegrationTest` passes locally for stream service; full persisted reconnect E2E requires Docker. |

No orphaned Phase 2 requirements found: ROADMAP maps Phase 2 to `CLOUD-01..CLOUD-06`, `E2E-01`, `E2E-04`, and `E2E-05`, and these IDs appear in plan frontmatter.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
| --- | --- | --- | --- | --- |
| `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java` | 67, 76 | `return null` for optional terminal-result/failure JSON columns | ℹ️ Info | Intentional nullable DB fields for non-completed/non-failed terminal payloads; not a stub. |
| `pom.xml` / effective Maven dependency tree | 29 / dependency tree | `testcontainers.version` property says `2.0.3`, but effective Testcontainers artifacts resolve to `1.21.4` | ⚠️ Warning | Not blocking Phase 2 behavior, but dependency-management order/BOM interaction should be cleaned up to match documented stack and plan acceptance. |
| `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/NoopRunActivationTrigger.java` | 7-14 | Conditional fallback no-op activation bean | ℹ️ Info | Safe due `@ConditionalOnMissingBean`; active product wiring supplies scheduler trigger. Keep an eye that production contexts always include `CloudRuntimeBeanConfiguration`. |

### Human Verification Required

### 1. Docker-enabled full Phase 2 gate

**Test:** On a Docker-enabled local/CI host, run `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test`.
**Expected:** All tests pass, including `JdbcPersistenceIntegrationTest`, `PostgresRunQueueTest`, `CloudServerHeadlessE2ETest`, and `RunCancellationIntegrationTest` using `PostgreSQLContainer<>("postgres:17-alpine")`.
**Why human:** This verifier environment has no Docker daemon/socket (`/var/run/docker.sock` missing), so Testcontainers cannot start PostgreSQL.

### 2. Optional live smoke test against packaged server

**Test:** Start the Spring Boot web adapter with test/dev auth and PostgreSQL, then create a session/run, connect to `/stream`, query `/events`, `/status`, `/result`, and cancel a running run.
**Expected:** API auth accepts dev/test headers, run reaches terminal state, SSE event ids match persisted sequence numbers, reconnect with `Last-Event-ID` replays only higher sequences, and cancellation appears in run status/events.
**Why human:** Requires a running server and database environment; automated equivalent exists but is Docker-gated here.

### Gaps Summary

No code gaps were found against the Phase 2 goal. The phase is implemented, wired, and non-Docker verified. The remaining blocker for declaring fully passed is environmental: Docker/Testcontainers PostgreSQL gates could not be executed here.

---

_Verified: 2026-06-14T06:28:00Z_
_Verifier: the agent (gsd-verifier)_
