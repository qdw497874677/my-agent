# Phase 02: Cloud Server, Persistence, SSE, and Baseline Security - Research

## RESEARCH COMPLETE

**Researched:** 2026-06-14  
**Domain:** Spring Boot MVC Cloud Server, PostgreSQL/Flyway/JDBC persistence, Server-Sent Events, baseline Spring Security, deterministic E2E  
**Confidence:** HIGH for Spring/PostgreSQL/Testcontainers patterns; MEDIUM-HIGH for exact module split and DB queue details because they depend on implementation choices in Phase 2.

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions

## Implementation Decisions

### Durability Depth
- **D-01:** Phase 2 is **checkpoint-ready**, not crash-resumable. It must implement persisted history, run/session/event/read-model queries, cancellation state, and SSE replay. It should reserve explicit checkpoint/replay-ready fields or extension points where natural, but it must not implement process-crash recovery of in-flight runs in this phase.
- **D-02:** The event store and state tables should be designed so later durable execution/replay can be added without breaking the public REST/SSE/event contract. Downstream agents should treat actual recovery workers/checkpoint replay as deferred scope.

### API Contract Shape
- **D-03:** REST API shape should be **Session-centric**. Prefer resources such as `/api/sessions`, `/api/sessions/{sessionId}/runs`, `/api/sessions/{sessionId}/runs/{runId}`, and nested history/event resources, while still making `runId` a stable first-class identifier in responses and queries.
- **D-04:** `pi-agent-client` owns external API DTOs, command/query DTOs, response envelopes, and SSE DTOs. `pi-agent-adapter-web` owns mapping between client DTOs and Domain/App models. Do not expose Domain records directly as the public JSON contract and do not add Jackson/Jakarta/Spring annotations to Domain.
- **D-05:** API must stay open to non-chat `RunInput` modes from Phase 1. Phase 2 may implement a minimal create-run request first, but should not encode the API as chat-transcript-only.

### SSE Streaming and Replay
- **D-06:** SSE uses **persist-then-emit** semantics: the runtime `RunEvent` is synchronously persisted successfully before it is emitted to active SSE subscribers. This prioritizes durable ordering and reconnect correctness over lowest possible latency.
- **D-07:** SSE replay is primarily based on run-scoped `RunEvent.sequence`. Support replay from a sequence cursor and map/accept `Last-Event-ID` where practical, but the canonical ordering contract is per-run monotonic sequence.
- **D-08:** SSE and event-history REST responses must expose the same provider-neutral event envelope semantics as persisted storage. Adapter DTOs may be JSON-friendly, but they must preserve event ID, run/session/workspace context, sequence, timestamp, type/wire name, trace/correlation/causation IDs, visibility, redaction metadata, and payload identity.

### Security Baseline
- **D-09:** Phase 2 security baseline is **dev auth + JWT-ready**. Provide a simple development/test principal path for local E2E, while configuring the Cloud Server boundary so production can run as an OAuth2 Resource Server / JWT-protected API without redesign.
- **D-10:** Every API call and emitted/persisted event path must populate tenant ID, user ID, session ID, run ID, trace ID, and correlation ID placeholders from the security/request context. Phase 2 can remain single-tenant in behavior, but identifiers must be present consistently.
- **D-11:** API/SSE responses may safely expose IDs such as `tenantId`, `userId`, `sessionId`, `runId`, `traceId`, and `correlationId` for debugging/correlation, but must not expose raw auth tokens, raw claims, sensitive headers, secrets, or unredacted sensitive payloads by default.

### Persistence Model and Ports
- **D-12:** Persistence/query ports belong in `pi-agent-app`, with JDBC/PostgreSQL implementations in `pi-agent-infrastructure`. Domain remains focused on framework-free runtime contracts and must not gain DB-specific repository concepts unless a later phase proves a narrow Domain contract is needed.
- **D-13:** PostgreSQL schema should combine an append-only `run_events` event log with query-friendly read models/tables for `sessions`, `runs`, `steps`, `messages`, `tool_calls`, and basic `audit_records`. The event log is the durable event history; read models make CLOUD-03 queries efficient and explicit.
- **D-14:** Use Flyway migrations from day one. Event payloads and flexible metadata may use JSONB in Infrastructure persistence, but JSONB shape must be mapped from explicit client/domain event DTOs rather than leaking internal Java class names.

### Execution Queue and Scheduling
- **D-15:** Abstract the run queue/scheduler boundary before implementation. Downstream agents should design an execution queue/dispatcher port so future in-memory, DB-backed, Redis, or external queue implementations can be swapped.
- **D-16:** The initial Phase 2 implementation may use a DB-backed queue/worker approach as the default implementation. This keeps the cloud execution model explicit and extensible without introducing Kafka/Rabbit/Redis in Phase 2.
- **D-17:** Apply the same pattern broadly: define extension-friendly abstractions first, keep the default implementation simple, and avoid hard-coding infrastructure choices into Domain or public contracts.

### the agent's Discretion
- Exact endpoint path names, DTO record names, pagination parameters, and HTTP status mapping are planner/researcher discretion as long as the Session-centric API, client DTO boundary, and public REST/SSE compatibility are preserved.
- Exact checkpoint-ready fields are planner discretion; they should be minimal and forward-compatible, not a hidden implementation of full crash replay.
- Exact DB queue polling/locking approach is planner/researcher discretion, but it must be testable with PostgreSQL/Testcontainers and preserve cancellation/event ordering.

### Deferred Ideas (OUT OF SCOPE)

## Deferred Ideas

- Full crash-resumable durable execution and replay workers — deferred beyond Phase 2 unless roadmap explicitly promotes v2 ADV-02.
- Real model provider/OpenAI-compatible streaming — Phase 3.
- Governed ToolExecutionGateway, policy/audit redaction depth, and real tool execution — Phase 4.
- Web Console/Admin Governance UI — Phase 5.
- Redis/Kafka/Rabbit/external queue implementation — future scaling option behind the Phase 2 queue abstraction.
- Full OAuth/RBAC/multi-tenant authorization depth — later hardening; Phase 2 is dev auth + JWT-ready with IDs consistently present.

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| CLOUD-01 | User can create an Agent Run through an authenticated REST API. | Use Spring MVC controllers in `pi-agent-adapter-web`, public request/response DTOs in `pi-agent-client`, app use cases in `pi-agent-app`, dev/JWT-ready Spring Security filter chain. |
| CLOUD-02 | User can stream RunEvents from an Agent Run through SSE using the same event envelope persisted by the platform. | Use `SseEmitter`/`text/event-stream`; implement persist-then-emit `EventSink` in Infrastructure; replay from `run_events.sequence`; map `Last-Event-ID` to sequence cursor. |
| CLOUD-03 | User can query run status, run detail, event history, step history, messages, tool calls, and terminal result through REST APIs. | Use read-model tables plus append-only `run_events`; define query ports in App and JDBC implementations in Infrastructure; public query DTOs in Client. |
| CLOUD-04 | User can cancel a running Agent Run through REST API and observe cancellation in run state and event stream. | Add cancellation app use case, cancellation token registry/port, DB state transition, audit record, and terminal `run.cancelled` event ordering tests. |
| CLOUD-05 | Cloud Server exposes baseline authentication/security context, tenant/user placeholders, request correlation IDs, structured logs, and health endpoints. | Use Spring Security dev profile + Resource Server shape, servlet filter for trace/correlation IDs, MDC/logback pattern, Actuator health/info endpoints. |
| CLOUD-06 | Cloud Server stores run/session/event/audit state durably using a PostgreSQL-backed implementation with migrations. | Use Spring JDBC/JdbcTemplate, Flyway versioned SQL migrations under Infrastructure, PostgreSQL JSONB for payload/metadata, Testcontainers PostgreSQL tests. |
| E2E-01 | Headless E2E can create a run through API/runtime entry points, stream events, persist state, and assert terminal status without real model keys. | Wire `pi-testkit` `GeneralAgentLoop` into Spring Boot test profile, use MockMvc/WebTestClient or HTTP client plus Testcontainers PostgreSQL. |
| E2E-04 | Headless E2E verifies cancellation, timeout, max-step, terminal events, and absence of hanging model/tool tasks. | Use fake runtime scripts/cancellation token, worker timeout/cancellation tests, terminal-event invariant from Phase 1, and executor shutdown assertions. |
| E2E-05 | Headless E2E verifies SSE ordering, terminal events, and reconnect/replay behavior using event sequence or lastEventId. | Use per-run monotonic sequence, `id:` field on SSE events, event history endpoint, reconnect from sequence/`Last-Event-ID`, and duplicate/missing event assertions. |

</phase_requirements>

## Summary

Phase 2 should be planned as an API-first Spring Boot 3.5.x modular-monolith slice around the Phase 1 Domain contracts. The phase should not mutate Domain to satisfy JSON, persistence, or Spring concerns. Instead, add public JSON/SSE DTOs in `pi-agent-client`, application use-case and port contracts in `pi-agent-app`, JDBC/Flyway/PostgreSQL implementations in `pi-agent-infrastructure`, and Spring MVC/SSE/security/controller mapping in `pi-agent-adapter-web`. `pi-testkit` remains the no-key deterministic runtime source for E2E.

The central architecture is **persist-then-emit**: every `RunEvent` from the runtime is written to the `run_events` append-only table and projected into read-model tables before it is sent to active SSE subscribers. SSE is therefore a durable event stream view, not an independent transient stream. Reconnect/replay should be run-scoped and sequence-based, with `Last-Event-ID` accepted as an adapter-level convenience. The canonical cursor is `RunEvent.sequence` per run.

The phase should introduce security and operations as placeholders with real boundaries: development/test authentication for local E2E, a SecurityFilterChain shaped like OAuth2 Resource Server/JWT for production, tenant/user/trace/correlation IDs on every request/event/audit row, Actuator health, and structured logs. It should not implement full RBAC, real model providers, governed tool gateway, Redis/Kafka, or crash-resumable durable replay.

**Primary recommendation:** Plan Phase 2 as five implementation waves: client API DTO contract → App use cases/ports → Infrastructure Flyway/JDBC/event store/DB queue → Web adapter REST/SSE/security/correlation → Testcontainers headless E2E and architecture gates.

## Project Constraints (from AGENTS.md)

No `./AGENTS.md` exists in `/root/workspace/pi-java` — verified by glob search on 2026-06-14. Project instructions from `CLAUDE.md` and planning context still apply:

- Java-first cloud Agent platform; avoid TypeScript/core CLI assumptions.
- COLA dependency direction: Adapter → App → Domain ← Infrastructure.
- Domain/Runtime Core must not depend on Spring, Jackson, Jakarta, JDBC/DB, Vaadin, PF4J, MCP, provider SDKs, or product UI.
- Workspace/file/command concerns remain behind Workspace abstractions; no direct host shell/filesystem assumptions.
- Tool calls require future Policy/Audit/Timeout/Approval/Sandbox extension points; Phase 2 must not bypass later governance.
- Every phase must have automated verification; key paths should use fake model/tool/runtime and Testcontainers.
- Cloud Server API is the priority; TUI/CLI are later clients over the same API.
- Existing ArchUnit rules must remain green; App currently may depend only on Java/App/Domain/Client/test packages.

## Project Skills Checked

- `.claude/skills/` — not present.
- `.agents/skills/` — not present.
- Loaded global `cola-architecture` skill because this phase adds App/Adapter/Infrastructure classes under COLA. Relevant rule: controllers belong in Adapter, orchestration/ports in App, DB implementations in Infrastructure, and Domain stays pure Java without Spring/DB annotations.

## Existing Code and Contracts to Reuse

### Must Reuse from Phase 1

| Existing file/package | Reuse in Phase 2 |
|-----------------------|------------------|
| `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java` | Canonical internal event envelope to persist and map to client/SSE DTOs. Preserve all IDs, sequence, timestamp, type, trace/correlation/causation, visibility, redaction. |
| `pi-agent-domain/.../RunEventPayload.java` | Source for explicit payload DTO mapping. Do not serialize with Java class names; map sealed payload variants to stable wire DTOs. |
| `pi-agent-domain/.../RunEventType.java` | Stable event wire-name taxonomy. Use event type wire names as SSE `event:` values if available/appropriate. |
| `pi-agent-domain/.../EventSink.java` | Implement Infrastructure sink that persists and then fans out to active SSE subscribers. |
| `pi-agent-domain/.../runtime/AgentRuntime.java` | App execution dispatcher invokes `start(RunContext)` and routes cancellation via `cancel(runId, reason)` plus cancellation token state. |
| `pi-agent-domain/.../runtime/RunContext.java`, `RunInput.java`, `Run.java`, `RunStatus.java`, `CancellationToken.java` | Use to construct runtime requests and expose status/read models without changing Domain. |
| `pi-agent-domain/.../session/Session.java`, `SessionEntry.java`, `SessionContextResolver.java` | Basis for session-centric API and session history/read-model reconstruction. |
| `pi-agent-domain/.../common/PlatformIds.java` | Typed IDs for tenant/user/session/run/workspace/trace/correlation/causation; map to strings in Client DTOs. |
| `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` | Deterministic `AgentRuntime` for headless E2E. It emits ordered events synchronously and terminal last. |
| `pi-testkit/.../FakeModelClient.java`, `FakeToolInvoker.java`, `FakePolicy.java`, `EventCollector.java`, `DeterministicIds.java`, `DeterministicClock.java` | Test profile fixtures for successful, cancellation, timeout, max-step, policy-blocked, and replay tests without model keys. |
| `pi-agent-app/src/test/java/.../AppDependencyArchTest.java` | Must evolve but preserve App boundary. App ports can depend on Domain/Client, not Spring/JDBC. |
| `docs/phase-01-domain-contracts.md` | Downstream boundary index; cite it in plan tasks so implementers do not re-decide Domain contracts. |

### Exact Files/Packages Likely Touched

**Parent/build:**
- `pom.xml` — add Spring Boot BOM/dependency management, PostgreSQL driver, Flyway, Spring dependencies, Testcontainers versions as properties or module-scoped deps.
- `pi-agent-adapter-web/pom.xml` — add `spring-boot-starter-web`, `spring-boot-starter-security`, `spring-boot-starter-actuator`, `spring-security-oauth2-resource-server`, `spring-security-oauth2-jose`, test starter.
- `pi-agent-infrastructure/pom.xml` — add `spring-boot-starter-jdbc`, `org.postgresql:postgresql`, `org.flywaydb:flyway-core`, and for modern Flyway PostgreSQL support also validate/add `org.flywaydb:flyway-database-postgresql` if required by chosen Flyway line.
- `pi-testkit/pom.xml` or `pi-agent-adapter-web` test scope — add Testcontainers PostgreSQL/JUnit Jupiter and Spring Boot test dependencies.

**`pi-agent-client` packages to create:**
- `io.github.pi_java.agent.client.api` — response envelope/page/cursor records.
- `io.github.pi_java.agent.client.session` — `CreateSessionRequest`, `SessionResponse`, `SessionHistoryResponse`.
- `io.github.pi_java.agent.client.run` — `CreateRunRequest`, `RunResponse`, `RunStatusResponse`, `CancelRunRequest`, `RunDetailResponse`, `RunResultResponse`.
- `io.github.pi_java.agent.client.event` — `RunEventDto`, `RunEventPayloadDto` variants, `EventHistoryResponse`, `SseRunEventDto` if separate.
- Keep Client DTOs serialization-friendly, but avoid leaking Domain classes directly. Jackson annotations are acceptable in Client/Adapter if needed, not Domain.

**`pi-agent-app` packages to create:**
- `io.github.pi_java.agent.app.api` or `...usecase` — `RunCommandService`, `RunQueryService`, `SessionCommandService`, `SessionQueryService`.
- `io.github.pi_java.agent.app.port.persistence` — `RunEventStore`, `RunStateRepository`, `SessionRepository`, `RunReadRepository`, `AuditRepository`, `CancellationRepository`.
- `io.github.pi_java.agent.app.port.execution` — `RunQueue`, `RunDispatcher`, `RunWorker`, `CancellationRegistry` or equivalent.
- `io.github.pi_java.agent.app.context` — `RequestContext`, `SecurityPrincipalContext`, `CorrelationContext` as framework-free App records.

**`pi-agent-infrastructure` packages to create:**
- `io.github.pi_java.agent.infrastructure.jdbc` — `JdbcRunEventStore`, `JdbcRunStateRepository`, read repositories, row mappers.
- `io.github.pi_java.agent.infrastructure.event` — `PersistingEventSink`, `SseEventBus`/publisher implementation adapter-side or infra-side behind an App port.
- `io.github.pi_java.agent.infrastructure.queue` — `PostgresRunQueue`, `PostgresRunDispatcher`, queue poller using row state and optionally `FOR UPDATE SKIP LOCKED`.
- `io.github.pi_java.agent.infrastructure.config` — Spring configuration for JDBC repositories/event sink/worker beans.
- `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` — initial Flyway migration.

**`pi-agent-adapter-web` packages to create:**
- `io.github.pi_java.agent.adapter.web.PiCloudServerApplication` — Spring Boot application entrypoint if this module becomes runnable.
- `io.github.pi_java.agent.adapter.web.controller` — `SessionController`, `RunController`, `RunEventStreamController`, `Health` only if not relying on Actuator.
- `io.github.pi_java.agent.adapter.web.mapper` — `RunDtoMapper`, `RunEventDtoMapper`, `SessionDtoMapper`.
- `io.github.pi_java.agent.adapter.web.security` — `SecurityConfig`, `DevAuthenticationFilter`, `PiPrincipal`, context extractor.
- `io.github.pi_java.agent.adapter.web.correlation` — correlation/trace servlet filter + MDC setup.
- `src/main/resources/application.yml`, `application-test.yml` — virtual threads, datasource, Flyway, Actuator, security profile toggles.

**Tests likely touched/created:**
- `pi-agent-adapter-web/src/test/java/.../RunApiIntegrationTest.java`
- `pi-agent-adapter-web/src/test/java/.../RunSseIntegrationTest.java`
- `pi-agent-infrastructure/src/test/java/.../JdbcRunEventStoreTest.java`
- `pi-agent-infrastructure/src/test/java/.../PostgresRunQueueTest.java`
- `pi-agent-adapter-web/src/test/java/.../SecurityAndCorrelationIntegrationTest.java`
- `pi-agent-adapter-web/src/test/java/.../CloudServerHeadlessE2ETest.java`
- New/updated ArchUnit tests for Adapter/App/Infrastructure boundaries.

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Java | 21.0.11 available locally | Runtime and build baseline | Existing project uses Maven compiler release 21; Phase 1 verification requires Java 21. |
| Maven | 3.8.7 available locally; stack recommendation is 3.9.x+ | Multi-module build | Existing project is Maven multi-module. 3.8.7 can run now but planners should note stack recommendation drift. |
| Spring Boot | 3.5.x; Context7 resolved `v3.5.9` docs | REST/SSE, security integration, Actuator, configuration | Stable Boot line recommended by stack research. Boot docs confirm virtual thread settings and Actuator. |
| Spring MVC | Spring Framework 6.2 managed by Boot 3.5 | REST controllers and `SseEmitter` | Official Spring MVC docs support `SseEmitter`/`ResponseBodyEmitter` for SSE streams. Simpler than full WebFlux. |
| Spring Security | 6.5 line managed by Boot 3.5 | Dev auth + JWT-ready API boundary | Official docs show `SecurityFilterChain` with `.oauth2ResourceServer(oauth2 -> oauth2.jwt())`; use this shape for production profile. |
| Spring JDBC / `JdbcTemplate` | Boot managed | Explicit SQL for run/event/read-model persistence | Fits append-heavy event store and query tables better than ORM. Avoid JPA for core state. |
| PostgreSQL | 17/18 target; Testcontainers can run current image | Durable system of record | JSONB, robust transactions, indexing, row locks, and `SKIP LOCKED` support queue implementation. |
| Flyway | Current line; verify exact artifact line at implementation | Versioned DB migrations | Required from day one; official docs confirm versioned SQL naming convention (`V...__description.sql`) and multiple migration locations. |
| Jackson | Boot managed | JSON DTO serialization in Client/Adapter/Infrastructure JSONB mapping | Use outside Domain. Avoid default polymorphic Java class names in persisted JSON. |
| Testcontainers Java | 2.0.3 from Context7 | PostgreSQL integration/E2E tests | Official docs show JUnit 5 PostgreSQL containers and dependency setup. Requires Docker availability. |
| JUnit Jupiter / AssertJ / ArchUnit | Existing: JUnit 5.10.3, AssertJ 3.26.3, ArchUnit 1.3.0 | Unit, integration, architecture tests | Already in project parent/test modules. Keep architecture gates green. |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Boot Actuator | Boot managed | Health/info/metrics endpoints | Required for CLOUD-05 health baseline. Expose `health,info,metrics` minimally. |
| Micrometer/MDC logging | Boot/logback managed | Correlation-aware logs | Use a servlet filter to populate MDC with tenant/user/session/run/trace/correlation where available. |
| `spring-security-oauth2-resource-server` + `spring-security-oauth2-jose` | Security 6.5 / Boot managed | JWT Resource Server readiness | Add even if dev profile uses a fake principal so production profile can switch to JWT without API redesign. |
| PostgreSQL JSONB GIN indexes | DB feature | Event payload/metadata query acceleration | Use selectively; first index typed columns (`run_id`, `sequence`, `type`, status), then JSONB expression indexes only for proven queries. |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Spring MVC + `SseEmitter` | WebFlux `Flux<ServerSentEvent<?>>` | WebFlux adds reactive complexity. Use only if future model/MCP streaming requires reactive backpressure end-to-end. |
| JDBC explicit SQL | JPA/Hibernate | ORM is friction for append-heavy events, JSONB payloads, explicit locking, and queue polling. Avoid for core state. |
| PostgreSQL-backed queue | Redis/Kafka/RabbitMQ | External brokers improve scale but add operations and scope. Phase 2 decision requires abstraction first, DB-backed implementation allowed. |
| Dev auth filter + JWT-ready config | Full OAuth/RBAC | Full authz is deferred. Implement boundaries and IDs now without overbuilding. |
| Persisted event replay | In-memory SSE buffer | In-memory buffer breaks reconnect and durability. Persisted sequence is the contract. |

**Installation / dependency skeleton:**

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-dependencies</artifactId>
      <version>${spring-boot.version}</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>

<!-- pi-agent-adapter-web -->
<dependency>org.springframework.boot:spring-boot-starter-web</dependency>
<dependency>org.springframework.boot:spring-boot-starter-security</dependency>
<dependency>org.springframework.boot:spring-boot-starter-actuator</dependency>
<dependency>org.springframework.security:spring-security-oauth2-resource-server</dependency>
<dependency>org.springframework.security:spring-security-oauth2-jose</dependency>

<!-- pi-agent-infrastructure -->
<dependency>org.springframework.boot:spring-boot-starter-jdbc</dependency>
<dependency>org.postgresql:postgresql</dependency>
<dependency>org.flywaydb:flyway-core</dependency>
<!-- validate exact Flyway line; add if required -->
<dependency>org.flywaydb:flyway-database-postgresql</dependency>

<!-- tests -->
<dependency>org.springframework.boot:spring-boot-starter-test</dependency>
<dependency>org.testcontainers:testcontainers-postgresql</dependency>
<dependency>org.testcontainers:testcontainers-junit-jupiter</dependency>
```

**Version verification:** Context7 official/source docs verified Spring Boot `v3.5.9`, Spring Framework `6.2`, Spring Security `6.5`, and Testcontainers `2.0.3`. Before coding, planner should add a Wave 0 task to verify exact Maven Central patch versions with `mvn versions:display-dependency-updates` or Maven Central because this environment should not rely on stale training data.

## Architecture Patterns

### Recommended Project Structure

```text
pi-agent-client/
  src/main/java/io/github/pi_java/agent/client/
    api/                 # ApiResponse, Page/Cursor DTOs, error DTOs
    session/             # Public session request/response DTOs
    run/                 # Public run command/query response DTOs
    event/               # Public event/SSE DTOs and payload variants

pi-agent-app/
  src/main/java/io/github/pi_java/agent/app/
    usecase/             # Create/cancel/query orchestration services
    port/persistence/    # Event store, repositories, read ports, audit port
    port/execution/      # RunQueue, RunDispatcher, cancellation registry
    context/             # Request/security/correlation context records

pi-agent-infrastructure/
  src/main/java/io/github/pi_java/agent/infrastructure/
    jdbc/                # JdbcTemplate repositories + row mappers
    event/               # Persisting EventSink and event projections
    queue/               # PostgreSQL-backed queue/worker implementation
    config/              # Spring infrastructure configuration
  src/main/resources/db/migration/
    V1__create_cloud_runtime_tables.sql

pi-agent-adapter-web/
  src/main/java/io/github/pi_java/agent/adapter/web/
    controller/          # Session, Run, Event stream REST/SSE controllers
    mapper/              # Client DTO <-> App/Domain mapping
    security/            # Dev auth + JWT-ready security config
    correlation/         # Trace/correlation filter + MDC
    config/              # Spring Boot app config

pi-testkit/
  src/main/java/io/github/pi_java/agent/testkit/
    # Existing fake runtime plus optional Spring test fixture helpers
```

### Pattern 1: Session-Centric REST API

**What:** Make sessions the top-level API surface and runs nested resources, while returning `runId` everywhere as a stable first-class ID.

**Recommended endpoints:**

```text
POST   /api/sessions
GET    /api/sessions/{sessionId}
GET    /api/sessions/{sessionId}/history

POST   /api/sessions/{sessionId}/runs
GET    /api/sessions/{sessionId}/runs/{runId}
GET    /api/sessions/{sessionId}/runs/{runId}/status
POST   /api/sessions/{sessionId}/runs/{runId}/cancel

GET    /api/sessions/{sessionId}/runs/{runId}/events?afterSequence=0&limit=500
GET    /api/sessions/{sessionId}/runs/{runId}/steps
GET    /api/sessions/{sessionId}/runs/{runId}/messages
GET    /api/sessions/{sessionId}/runs/{runId}/tool-calls
GET    /api/sessions/{sessionId}/runs/{runId}/stream  (produces text/event-stream)
```

**Why:** It satisfies D-03, keeps future Web Console/TUI/CLI protocol unified, and avoids run-only APIs that cannot represent conversation/history continuation.

### Pattern 2: Client DTO Boundary and Adapter Mapping

**What:** `pi-agent-client` owns public JSON records. `pi-agent-adapter-web` maps to/from App/Domain. Domain records are not controller responses.

**Example:**

```java
// pi-agent-client
public record RunEventDto(
    String eventId,
    String tenantId,
    String userId,
    String sessionId,
    String runId,
    String stepId,
    String workspaceId,
    long sequence,
    Instant timestamp,
    String type,
    String traceId,
    String correlationId,
    String causationId,
    String visibility,
    RedactionDto redaction,
    RunEventPayloadDto payload
) {}

// pi-agent-adapter-web
RunEventDto toDto(RunEvent event) {
    return new RunEventDto(
        event.eventId(), event.tenantId().value(), event.userId().value(),
        event.sessionId().value(), event.runId().value(), event.stepId().value(),
        event.workspaceId().value(), event.sequence(), event.timestamp(),
        event.type().wireName(), event.traceId().value(),
        event.correlationId().value(), event.causationId().value(),
        event.visibility().name(), toDto(event.redaction()), toPayloadDto(event.payload())
    );
}
```

**Pitfall avoided:** no Jackson polymorphic class names from Domain sealed interfaces in REST or JSONB.

### Pattern 3: Persist-Then-Emit Event Pipeline

**What:** Runtime publishes to one `EventSink`; Infrastructure sink writes event + projections in a DB transaction; only after commit does it notify active SSE subscribers.

**Recommended flow:**

```text
AgentRuntime -> EventSink.publish(RunEvent)
  -> transaction begin
     -> insert run_events(run_id, sequence, type, payload_jsonb, ...)
     -> upsert runs/steps/messages/tool_calls read models as applicable
     -> insert audit_records for security-sensitive actions where applicable
  -> transaction commit
  -> in-process EventBus.publish(dto/event) to SseEmitter subscribers
```

**Important:** If DB insert fails, do not emit SSE. If SSE emit fails, keep DB event and complete/remove that subscriber. This makes reconnect correct.

### Pattern 4: SSE Replay and Last-Event-ID Mapping

**What:** `GET .../stream?afterSequence=N` first replays persisted events where `sequence > N`, then subscribes to live fanout. If `Last-Event-ID` exists, parse it into a sequence cursor.

**SSE event shape:**

```java
SseEmitter.SseEventBuilder builder = SseEmitter.event()
    .id(String.valueOf(event.sequence()))      // or "{runId}:{sequence}" if globally safer
    .name(event.type())                        // e.g. run.started, model.delta
    .data(runEventDto);
emitter.send(builder);
```

**Source:** Spring Framework 6.2 docs confirm `SseEmitter` is a `ResponseBodyEmitter` subclass for W3C SSE and supports asynchronous sends; default timeouts can be set on `SseEmitter`.

### Pattern 5: App Ports for Queue/Scheduler First

**What:** Define a queue/dispatcher boundary in App; implement simple PostgreSQL-backed worker in Infrastructure. Do not put queue mechanics in Domain or controllers.

**App ports:**

```java
public interface RunQueue {
    void enqueue(QueuedRun queuedRun);
    Optional<QueuedRun> claimNext(WorkerId workerId, Instant now);
    void markRunning(String runId, Instant startedAt);
    void markTerminal(String runId, RunStatus status, Instant finishedAt);
}

public interface RunDispatcher {
    void dispatch(String runId);
}
```

**DB-backed implementation guidance:** Use a `run_queue` table or fields on `runs` (`queued_at`, `lease_owner`, `lease_until`, `attempt_count`). Claim work in a transaction with row-level locking, preferably `FOR UPDATE SKIP LOCKED` for concurrent workers. Keep it single-node friendly in Phase 2, but schema should not block later distributed workers.

### Pattern 6: Dev Auth + JWT-Ready Security

**What:** Provide a test/dev principal path for E2E, while production profile uses the standard Resource Server shape.

**Spring Security resource-server shape from official docs:**

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
}
```

**Dev/test profile:** use a filter or authentication provider that creates a principal from safe headers such as `X-Pi-Dev-User` and `X-Pi-Dev-Tenant`, enabled only under `dev`/`test`. Never echo raw auth headers or JWT claims in API/SSE.

### Anti-Patterns to Avoid

- **Domain JSON annotations:** violates Phase 1 and D-04; put serialization in Client/Adapter/Infrastructure mappers.
- **SSE before persistence:** creates events that clients can observe but cannot replay after reconnect.
- **Global in-memory event sequence:** sequence is per-run; enforce uniqueness with `(run_id, sequence)`.
- **In-memory-only cancellation:** REST cancel must update durable run state/history and produce observable event; token cancellation alone is insufficient.
- **Controller business logic:** controllers should extract request/security context and delegate to App use cases.
- **JPA for event store:** avoid ORM impedance for append-only JSONB events and queue locks.
- **Full OAuth/RBAC in Phase 2:** shape for JWT now, defer full authorization depth.
- **Docker-dependent tests with no skip/fallback story:** Testcontainers requires Docker; environment audit found Docker missing locally.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP REST routing/serialization | Custom HTTP server or manual JSON parser | Spring Boot MVC + Jackson in Adapter/Client | Standard, tested, integrates with Security/Actuator. |
| SSE protocol framing | Manual chunked response writer | Spring MVC `SseEmitter` | Handles W3C SSE formatting and async send lifecycle. |
| Password/JWT/security filters from scratch | Custom token verifier | Spring Security Resource Server for production shape; dev auth only in dev/test profile | Avoids auth vulnerabilities and keeps JWT-ready path. |
| Schema migrations | Runtime DDL or ad-hoc SQL scripts | Flyway versioned migrations | Repeatable DB evolution for CI/prod. |
| DB access abstraction | Custom connection pooling | Spring JDBC/JdbcTemplate + Boot DataSource/Hikari | Standard transaction and pool integration. |
| PostgreSQL in tests | H2 pretending to be PostgreSQL | Testcontainers PostgreSQL | JSONB/locking/Flyway behavior must be tested against real PostgreSQL. |
| Distributed broker in Phase 2 | Kafka/Rabbit/Redis | App queue port + PostgreSQL-backed implementation | Keeps abstraction without extra ops scope. |
| Correlation IDs only in logs | Unstructured log messages | Request filter + MDC + DTO/event/audit columns | Correlation must cross API/SSE/DB/logs. |

**Key insight:** Phase 2 complexity is not inventing web/persistence primitives; it is preserving the Phase 1 event/runtime contract across REST, durable storage, SSE replay, cancellation, and security context without leaking outer-framework concerns inward.

## Persistence and Schema Guidance

### Required Tables

Use Flyway `V1__create_cloud_runtime_tables.sql` to create at least:

| Table | Purpose | Key columns |
|-------|---------|-------------|
| `sessions` | Session metadata/read model | `tenant_id`, `user_id`, `session_id PK`, `workspace_id`, `current_entry_id`, `status`, `created_at`, `updated_at`, `metadata jsonb` |
| `session_entries` | Optional Phase 1 session tree projection if needed for history | `entry_id PK`, `session_id`, `parent_entry_id`, `sequence`, `payload_type`, `payload jsonb`, timestamps |
| `runs` | Run read model/status | `run_id PK`, `session_id`, `tenant_id`, `user_id`, `workspace_id`, `status`, `input_type`, `input jsonb`, `terminal_result jsonb`, `failure jsonb`, `trace_id`, `correlation_id`, timestamps, `cancel_requested_at`, `cancel_reason`, checkpoint-ready fields |
| `run_events` | Append-only event log | `event_id PK`, `run_id`, `session_id`, `tenant_id`, `user_id`, `workspace_id`, `step_id`, `sequence`, `event_type`, `timestamp`, `trace_id`, `correlation_id`, `causation_id`, `visibility`, `redaction jsonb`, `payload_schema`, `payload_version`, `payload jsonb` |
| `steps` | Step read model | `step_id`, `run_id`, `status`, `kind`, timestamps, `summary jsonb`; PK can be `(run_id, step_id)` |
| `messages` | Message read model | `message_id PK`, `session_id`, `run_id`, `role`, `content jsonb`, timestamps, redaction fields |
| `tool_calls` | Tool call read model placeholder | `tool_call_id PK`, `run_id`, `step_id`, `tool_name`, `status`, `arguments jsonb`, `result jsonb`, timestamps, redaction fields |
| `audit_records` | Basic security-sensitive audit | `audit_id PK`, `tenant_id`, `user_id`, `action`, `resource_type`, `resource_id`, `run_id`, `session_id`, `trace_id`, `correlation_id`, `timestamp`, `details jsonb` |
| `run_queue` or queue fields | DB-backed execution scheduling | `run_id PK`, `status`, `available_at`, `lease_owner`, `lease_until`, `attempt_count`, timestamps |

### Indexes and Constraints

- `run_events`: `UNIQUE(run_id, sequence)`, index `(run_id, sequence)`, `(session_id, run_id)`, `(tenant_id, user_id, created_at/timestamp)`.
- `runs`: indexes `(session_id, created_at desc)`, `(tenant_id, user_id, status)`, `(status, created_at)` for worker/query.
- `steps/messages/tool_calls`: indexes `(run_id, created_at/sequence)`.
- `audit_records`: indexes `(tenant_id, user_id, timestamp desc)`, `(run_id)`, `(correlation_id)`.
- JSONB: start without broad GIN indexes unless query plans require them. Official PostgreSQL docs confirm GIN indexes on JSONB and expression indexes for nested keys; use only for stable query patterns.

### Payload JSON Rules

- Store explicit `payload_schema` and `payload_version` columns or fields.
- Store payload JSON from `RunEventDto`/mapped payload DTO, not Java `RunEventPayload` class names.
- Preserve `visibility` and `redaction` on every event even if Phase 2 redaction is shallow.
- Include checkpoint-ready nullable columns such as `checkpoint_ref`, `resume_token`, `last_event_sequence`, or `runtime_snapshot_ref` only if natural; do not implement recovery worker.

## SSE, Ordering, Reconnect, and Cancellation Guidance

### Ordering

- Per-run sequence is canonical and must be monotonic.
- Enforce with DB `UNIQUE(run_id, sequence)`.
- If the Phase 1 fake runtime supplies sequence, persist it. If future runtimes do not, App/Infrastructure needs a sequence allocator per run; do not allocate globally.
- Terminal event must be last for completed/cancelled/failed/policy-blocked paths. Preserve Phase 1 invariant in E2E.

### Reconnect/Replay

- REST history: `GET .../events?afterSequence=N&limit=L` returns persisted events ordered by sequence.
- SSE stream: on connect, determine cursor from `afterSequence` query param or `Last-Event-ID`; replay persisted rows with sequence greater than cursor; then subscribe to live bus.
- Set SSE `id` to sequence or `runId:sequence`. If using `runId:sequence`, document parser and accept bare numeric sequence too for client simplicity.
- Send heartbeat/comment events for long-running streams if tests or proxies need liveness; do not persist heartbeat as `RunEvent` unless it is a real domain event.

### Cancellation

- REST cancel should be idempotent: cancelling an already terminal run returns current terminal state without creating duplicate terminal events.
- Durable actions: set cancel requested fields on `runs`, write audit record, signal in-memory cancellation token/registry if active, and ensure a `run.cancelled` event appears if runtime observes cancellation.
- For queued runs, cancel before worker starts and emit terminal cancellation from App/worker path.
- For running fake runtime, cancellation can be cooperative. E2E should include a fake script that blocks or steps long enough to observe cancel.

## Queue/Scheduler Architecture

**Recommended Phase 2 approach:** define App-level queue/dispatcher ports first, implement an Infrastructure PostgreSQL-backed queue as the default, and keep worker execution simple.

### Flow

```text
POST create run
  -> App validates context/input
  -> transaction: insert session/run + run_events(run.created/queued) + audit + enqueue
  -> return 202 Accepted or 201 Created with runId/status

Worker/dispatcher
  -> claim queued run from DB
  -> mark running + publish run.started via EventSink
  -> build RunContext from persisted request/session/security/workspace context
  -> AgentRuntime.start(context)
  -> EventSink persists emitted events/projections
  -> mark terminal state
```

### DB Claiming

PostgreSQL supports row-level locking and `SKIP LOCKED`; use it for multiple worker readiness:

```sql
WITH next_run AS (
  SELECT run_id
  FROM run_queue
  WHERE status = 'QUEUED'
    AND available_at <= now()
  ORDER BY available_at, created_at
  FOR UPDATE SKIP LOCKED
  LIMIT 1
)
UPDATE run_queue q
SET status = 'CLAIMED', lease_owner = ?, lease_until = now() + interval '30 seconds'
FROM next_run
WHERE q.run_id = next_run.run_id
RETURNING q.*;
```

**Confidence:** MEDIUM-HIGH. `FOR UPDATE SKIP LOCKED` is a common PostgreSQL queue primitive, but exact implementation should be tested under Testcontainers for cancellation/order semantics.

## Security, Tenant/User, and Correlation Context

### Required Context Fields

Every API call, App command, persisted event, audit row, and response should carry:

- `tenantId`
- `userId`
- `sessionId` where applicable
- `runId` where applicable
- `workspaceId` where applicable
- `traceId`
- `correlationId`
- `causationId` for event chains

### Implementation Guidance

- Add a servlet filter early in the chain that reads/generates `X-Request-ID`/`X-Correlation-ID` and trace ID, stores them in request attributes and MDC, and returns response headers.
- Add a security context adapter that maps dev principal/JWT principal to App `RequestContext` without leaking raw claims.
- For dev/test profile, require explicit safe dev headers or use defaults (`dev-tenant`, `dev-user`) only in test profile.
- For production profile, configure Resource Server JWT. Official Spring Security docs require `spring-security-oauth2-resource-server` and `spring-security-oauth2-jose` for JWT bearer tokens.
- Permit `/actuator/health` and `/actuator/info` unauthenticated; require auth for `/api/**`; deny unknown paths.
- Disable CSRF for stateless API if no browser session/cookie auth is used in Phase 2.

## Common Pitfalls

### Pitfall 1: Serializing Domain sealed payloads directly
**What goes wrong:** Jackson may require annotations, expose Java type names, or couple public API to Domain internals.  
**Why it happens:** Fast path from controller returning `RunEvent`.  
**How to avoid:** Public DTOs in `pi-agent-client`; explicit mapper in Adapter.  
**Warning signs:** `@JsonTypeInfo` or `@JsonSubTypes` added under `pi-agent-domain`.

### Pitfall 2: SSE stream is not replayable
**What goes wrong:** User reconnects and misses events, terminal state, or cancellation.  
**Why it happens:** events are emitted from memory before persistence or buffered only in process.  
**How to avoid:** Persist-then-emit, replay from `run_events` ordered by `(run_id, sequence)`, map `Last-Event-ID`.  
**Warning signs:** SSE tests pass only when subscriber is connected before run starts.

### Pitfall 3: Duplicate or out-of-order terminal events
**What goes wrong:** Run appears both completed and cancelled, or terminal event is not last.  
**Why it happens:** REST cancellation, worker failure, and runtime terminal status race.  
**How to avoid:** transactional state transition guards; terminal state idempotency; `WHERE status NOT IN (...)` updates; tests for concurrent cancel.  
**Warning signs:** multiple `run.*` terminal events for one run in `run_events`.

### Pitfall 4: App layer imports Spring/JDBC by convenience
**What goes wrong:** COLA boundary breaks and future embedding/CLI/testability suffer.  
**Why it happens:** use-case services annotated or repositories implemented in App.  
**How to avoid:** App defines plain Java ports/use cases; Infrastructure implements; Adapter wires with Spring.  
**Warning signs:** `org.springframework..` imports in `pi-agent-app/src/main/java`.

### Pitfall 5: Testcontainers assumed available everywhere
**What goes wrong:** local verification fails because Docker is absent.  
**Why it happens:** integration tests require Docker but environment audit found `docker` command missing.  
**How to avoid:** plan Docker installation/CI requirement; keep fast unit/architecture tests runnable without Docker; mark integration profile separately if needed.  
**Warning signs:** `mvn test` always fails locally before reaching code due to missing Docker.

### Pitfall 6: Security context present in controllers but missing in events/audit
**What goes wrong:** persisted events cannot be correlated or tenant-filtered later.  
**Why it happens:** context not passed into App/RunContext/EventSink.  
**How to avoid:** App `RequestContext` is mandatory input to create/cancel/query; event persistence validates IDs non-null.  
**Warning signs:** rows with null/placeholder inconsistency for `tenant_id`, `user_id`, `trace_id`, `correlation_id`.

### Pitfall 7: Queue implementation leaks into public contract
**What goes wrong:** switching to Redis/Kafka later breaks APIs or Domain types.  
**Why it happens:** controller returns queue-specific states or Domain knows DB lease fields.  
**How to avoid:** keep queue state internal; expose run status (`QUEUED/RUNNING/TERMINAL`) only.  
**Warning signs:** `lease_owner`, `attempt_count`, broker names in Client DTOs.

## Code Examples

Verified patterns from official sources and project contracts:

### Spring MVC SSE with `SseEmitter`

```java
@GetMapping(path = "/api/sessions/{sessionId}/runs/{runId}/stream",
        produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamRun(@PathVariable String sessionId,
                            @PathVariable String runId,
                            @RequestParam(defaultValue = "0") long afterSequence,
                            @RequestHeader(name = "Last-Event-ID", required = false) String lastEventId) {
    long cursor = cursorResolver.resolve(afterSequence, lastEventId);
    SseEmitter emitter = new SseEmitter(0L); // or configured timeout
    runEventStreamService.replayThenSubscribe(sessionId, runId, cursor, event -> {
        emitter.send(SseEmitter.event()
                .id(Long.toString(event.sequence()))
                .name(event.type())
                .data(event));
    }, emitter::completeWithError, emitter::complete);
    return emitter;
}
```

Source: Spring Framework 6.2 MVC async docs confirm `SseEmitter` for W3C SSE and async `send`; project-specific cursor/persistence pattern from D-06..D-08.

### Spring Security JWT-Ready Filter Chain

```java
@Bean
SecurityFilterChain apiSecurity(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll())
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
    return http.build();
}
```

Source: Spring Security 6.5 Resource Server JWT docs.

### Testcontainers PostgreSQL JUnit 5

```java
@Testcontainers
class JdbcRunEventStoreTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

Source: Testcontainers Java 2.0.3 docs show `PostgreSQLContainer`, JUnit Jupiter `@Container`, and JDBC properties.

### Persist-Then-Emit EventSink Skeleton

```java
public final class PersistingEventSink implements EventSink {
    private final TransactionTemplate tx;
    private final RunEventStore eventStore;
    private final RunProjectionWriter projections;
    private final RunEventFanout fanout;

    @Override
    public void publish(RunEvent event) {
        tx.executeWithoutResult(status -> {
            eventStore.append(event);
            projections.apply(event);
        });
        fanout.publish(event); // after commit only
    }
}
```

Source: project D-06 persist-then-emit decision; implementation should ensure after-commit semantics if using Spring transactions with callbacks.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Local-only agent loop with in-memory events | Cloud API-first run/session lifecycle with durable event log and SSE | Project Phase 2 | Prevents CLI/TUI/local assumptions from shaping core. |
| Token-only stream | Provider-neutral `RunEvent` envelope stream | Phase 1 contract | SSE/Admin/TUI/CLI/audit all consume same event semantics. |
| In-memory event replay | PostgreSQL append-only event store + sequence replay | Phase 2 decision D-06/D-07 | Reconnect correctness and persistent history. |
| Crash-resumable replay now | Checkpoint-ready schema only | Phase 2 decision D-01 | Avoids overbuilding durable execution while reserving evolution path. |
| Full broker from day one | Queue abstraction + PostgreSQL-backed implementation | Phase 2 decision D-15/D-16 | Keeps cloud execution explicit without Kafka/Redis/Rabbit scope. |
| Full OAuth/RBAC now | Dev auth + JWT-ready boundary | Phase 2 decision D-09 | Enables E2E and production migration path without full authz hardening. |

**Deprecated/outdated for this project:**
- H2-only persistence tests: insufficient for JSONB, Flyway PostgreSQL, row locking, and queue semantics.
- Returning Domain records from controllers: violates D-04 and Phase 1 serialization-neutral contract.
- Full WebFlux architecture by default: not needed for Phase 2 and increases complexity.

## Runtime State Inventory

Phase 2 is not a rename/refactor/migration phase. Runtime State Inventory is omitted by trigger rule.

## Open Questions

1. **Should `pi-agent-adapter-web` be the runnable Spring Boot application module or should a separate `pi-cloud-server` module be introduced?**
   - What we know: current modules include `pi-agent-adapter-web` only; roadmap/stack older layout mentioned `pi-cloud-server`, but current context says adapter-web hosts Spring Boot REST/SSE.
   - What's unclear: whether maintainers want a separate deployable module now.
   - Recommendation: keep Phase 2 in `pi-agent-adapter-web` to minimize module churn; revisit a separate server module after API stabilizes.

2. **Exact event payload DTO discriminators.**
   - What we know: Domain payloads are sealed, but public JSON must avoid Java class names.
   - What's unclear: preferred discriminator field name (`kind`, `schema`, `type`) and payload version strategy.
   - Recommendation: use `payload.type` or top-level `payloadSchema` plus `payloadVersion`, with explicit record variants in Client.

3. **DB queue granularity.**
   - What we know: queue abstraction required; DB-backed allowed.
   - What's unclear: separate `run_queue` table vs queue fields on `runs`.
   - Recommendation: separate `run_queue` table for cleaner lease/attempt lifecycle and to keep `runs` as read model/status.

4. **Docker availability for local verification.**
   - What we know: `docker` is missing in this environment; Testcontainers cannot run locally here.
   - What's unclear: CI runner availability.
   - Recommendation: plan a Docker/CI prerequisite or split unit gates from integration gates; do not replace PostgreSQL integration tests with H2.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|-------------|-----------|---------|----------|
| Java 21 | Build/runtime/tests | ✓ | OpenJDK 21.0.11 | — |
| Maven | Build/tests | ✓ | 3.8.7 | Works now; stack prefers 3.9.x+ for future enterprise baseline. |
| Docker | Testcontainers PostgreSQL E2E | ✗ | — | Use CI/runner with Docker or install Docker; no equivalent fallback for PostgreSQL semantics. |
| PostgreSQL server/client (`psql`, `pg_isready`) | Manual local DB inspection | ✗ | — | Testcontainers/CI when Docker available; managed dev DB if configured. |
| Node/npm | GSD tooling only | ✓ | Node v22.22.2, npm 10.9.7 | — |
| Python 3 | Utility scripts only | ✓ | 3.12.3 | — |

**Missing dependencies with no fallback:**
- Docker is missing. Testcontainers-based automated PostgreSQL integration/E2E cannot execute in this local environment until Docker or a compatible container runtime is available.

**Missing dependencies with fallback:**
- `psql`/`pg_isready` are missing. They are helpful for manual inspection but not required if JDBC/Testcontainers tests run in CI.

## Validation Architecture

Nyquist validation is enabled (`.planning/config.json` has `workflow.nyquist_validation: true`).

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit Jupiter 5.10.3, AssertJ 3.26.3, ArchUnit 1.3.0; add Spring Boot Test and Testcontainers Java 2.0.3 for Phase 2 |
| Config file | Maven parent `pom.xml`; no separate Surefire config beyond parent plugin management |
| Quick run command | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-domain,pi-agent-app,pi-agent-client -am` |
| Adapter unit/API slice command | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-adapter-web -am -Dtest='*ControllerTest,*MapperTest,*Security*Test'` |
| Infrastructure integration command | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl pi-agent-infrastructure -am -Dtest='*Jdbc*Test,*Flyway*Test,*Queue*Test'` (requires Docker/Testcontainers) |
| Full suite command | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| CLOUD-01 | Authenticated REST can create run under a session and return IDs/status | Spring MVC integration | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunApiIntegrationTest#createRunRequiresAuthAndPersistsQueuedRun` | ❌ Wave 0 |
| CLOUD-02 | SSE emits persisted `RunEventDto` envelope | Spring MVC SSE integration + DB | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunSseIntegrationTest#streamEmitsPersistedEnvelope` | ❌ Wave 0 |
| CLOUD-03 | REST queries status/detail/events/steps/messages/tool calls/result | Controller + repository integration | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunQueryIntegrationTest` | ❌ Wave 0 |
| CLOUD-04 | Cancel REST changes durable state and appears in event history/SSE | E2E integration | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=RunCancellationIntegrationTest` | ❌ Wave 0 |
| CLOUD-05 | Auth/security context, tenant/user placeholders, correlation IDs, health | Security/filter integration | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=SecurityAndCorrelationIntegrationTest` | ❌ Wave 0 |
| CLOUD-06 | Flyway creates PostgreSQL tables and JDBC persists/query state | Testcontainers repository integration | `mvn -q test -pl pi-agent-infrastructure -am -Dtest=JdbcPersistenceIntegrationTest` | ❌ Wave 0 |
| E2E-01 | Create/stream/persist/query terminal status without real model keys | Full headless E2E | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudServerHeadlessE2ETest#successfulRunCreateStreamPersistQuery` | ❌ Wave 0 |
| E2E-04 | Cancellation/timeout/max-step terminal events and no hanging tasks | Full headless E2E | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudServerHeadlessE2ETest#cancelAndTimeoutPathsTerminate` | ❌ Wave 0 |
| E2E-05 | SSE ordering, terminal last, reconnect/replay by sequence/Last-Event-ID | Full headless E2E | `mvn -q test -pl pi-agent-adapter-web -am -Dtest=CloudServerHeadlessE2ETest#sseReconnectReplaysWithoutGaps` | ❌ Wave 0 |

### Sampling Rate

- **Per task commit:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q test -pl <touched-module> -am`
- **Per wave merge:** targeted module suite plus any integration tests introduced in that wave.
- **Phase gate:** `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` plus Testcontainers-backed E2E green in Docker-enabled environment.

### Wave 0 Gaps

- [ ] Parent/module POMs — add Spring Boot/Testcontainers/PostgreSQL/Flyway dependencies and profiles.
- [ ] `pi-agent-client` DTO package — required before controllers and persistence JSON mapping stabilize.
- [ ] `pi-agent-app` ports/use-case interfaces — required before Infrastructure/Adapter implementation.
- [ ] `pi-agent-infrastructure/src/main/resources/db/migration/V1__create_cloud_runtime_tables.sql` — covers CLOUD-06.
- [ ] `pi-agent-infrastructure/src/test/java/.../JdbcPersistenceIntegrationTest.java` — covers CLOUD-06.
- [ ] `pi-agent-infrastructure/src/test/java/.../PostgresRunQueueTest.java` — covers D-15/D-16 and cancellation ordering.
- [ ] `pi-agent-adapter-web/src/test/java/.../RunApiIntegrationTest.java` — covers CLOUD-01/CLOUD-03.
- [ ] `pi-agent-adapter-web/src/test/java/.../RunSseIntegrationTest.java` — covers CLOUD-02/E2E-05.
- [ ] `pi-agent-adapter-web/src/test/java/.../SecurityAndCorrelationIntegrationTest.java` — covers CLOUD-05.
- [ ] `pi-agent-adapter-web/src/test/java/.../CloudServerHeadlessE2ETest.java` — covers E2E-01/E2E-04/E2E-05.
- [ ] Docker/Testcontainers availability in CI or local environment — currently missing locally.

## Sources

### Primary (HIGH confidence)

- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Phase 2 locked decisions D-01..D-17, module boundaries, deferred scope.
- `.planning/REQUIREMENTS.md` — CLOUD-01..CLOUD-06, E2E-01, E2E-04, E2E-05.
- `.planning/ROADMAP.md` — Phase 2 goal and success criteria.
- `docs/phase-01-domain-contracts.md` — Phase 1 downstream contract index.
- Context7 `/spring-projects/spring-boot/v3.5.9` — virtual threads properties, Actuator/configuration evidence.
- Context7 `/websites/spring_io_spring-framework_reference_6_2` — Spring MVC `SseEmitter`/`ResponseBodyEmitter` async SSE evidence.
- Context7 `/websites/spring_io_spring-security_reference_6_5` — OAuth2 Resource Server JWT `SecurityFilterChain` evidence.
- Context7 `/testcontainers/testcontainers-java/2.0.3` — PostgreSQLContainer/JUnit Jupiter evidence.
- Context7 `/flyway/flyway` — versioned SQL migration naming and PostgreSQL migration evidence.
- Context7 `/websites/postgresql_current` — JSONB and GIN index evidence.

### Secondary (MEDIUM confidence)

- `.planning/research/STACK.md` — project stack guidance for Spring MVC, PostgreSQL/JDBC/Flyway, Spring Security, Testcontainers.
- `.planning/research/SUMMARY.md` — Phase 2 rationale and platform-level anti-features.
- Global `cola-architecture` skill — COLA placement guidance consistent with project architecture.

### Tertiary (LOW confidence)

- None used for critical Phase 2 recommendations.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Spring Boot/Spring MVC/Security/PostgreSQL/Flyway/Testcontainers guidance verified with Context7 and project stack docs.
- Architecture: HIGH — constrained by Phase 2 user decisions, Phase 1 contracts, COLA project rules, and existing module structure.
- Persistence schema: MEDIUM-HIGH — table categories are locked by requirements/context; exact columns and queue table shape need implementation validation.
- SSE/replay: HIGH — Spring SSE mechanism verified; persist-then-emit and sequence replay locked by user decisions.
- Queue/scheduler: MEDIUM-HIGH — abstraction is locked; PostgreSQL `SKIP LOCKED` implementation is standard but must be tested under concurrency/cancellation.
- Security baseline: HIGH — JWT Resource Server pattern verified; dev auth details are project-specific.
- Validation: MEDIUM-HIGH — test categories are clear; Docker absence blocks local Testcontainers execution until environment is fixed.

**Research date:** 2026-06-14  
**Valid until:** 2026-07-14 for Spring/PostgreSQL patterns; re-check exact Maven versions before implementation.
