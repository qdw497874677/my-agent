# Phase 2: Cloud Server, Persistence, SSE, and Baseline Security - Context

**Gathered:** 2026-06-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 2 proves Pi as a Cloud Server product by exposing the Phase 1 runtime contracts through Spring Boot REST/SSE, durable PostgreSQL-backed state, baseline security context, and a first headless E2E path. It must support authenticated run creation, status/detail/history queries, cancellation, SSE streaming/replay of persisted `RunEvent` envelopes, migrations for run/session/event/read-model/audit basics, health/correlation/logging baseline, and deterministic fake-runtime E2E without real model keys.

This phase does **not** implement real model providers, the governed ToolExecutionGateway, Web Console/Admin UI, SPI/Spring extension discovery, MCP, dynamic plugins, unrestricted shell/file tools, or full crash-resumable execution replay.

</domain>

<decisions>
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

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 2 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 2 — Phase goal, CLOUD/E2E requirement mapping, success criteria, and API-first note.
- `.planning/REQUIREMENTS.md` §Cloud Server API — CLOUD-01..CLOUD-06 details for REST/SSE, cancellation, security context, and PostgreSQL persistence.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E-01, E2E-04, and E2E-05 headless E2E, cancellation/timeout, and SSE ordering/reconnect requirements.
- `.planning/PROJECT.md` — Product constraints: Java-first, COLA boundaries, cloud safety, Workspace boundary, extensibility, verification, Cloud Server priority, and reference boundary.
- `.planning/STATE.md` — Current Phase 2 state and Phase 1 decisions carried forward.

### Prior Phase Contracts
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Locked Phase 1 decisions for RunEvent envelope, runtime input, session tree, workspace boundary, module/API exposure, and test gates.
- `docs/phase-01-domain-contracts.md` — Concrete downstream contract index for modules, `AgentRuntime`, `RunContext`, `RunEvent`, `Session`, Workspace, and fake testkit; also lists Phase 2 deferred items and Java 21 verification command.

### Architecture and Stack Guidance
- `.planning/research/SUMMARY.md` §Phase 2 — Recommended Spring Boot REST/SSE, PostgreSQL, persistence, security, and E2E rationale.
- `.planning/research/STACK.md` §Cloud Server API Layer — Spring MVC + SSE guidance and DTO/serialization considerations.
- `.planning/research/STACK.md` §Persistence, State, and Search — PostgreSQL, JDBC/Spring Data JDBC, Flyway, and avoidance of JPA for core run/event persistence.
- `.planning/research/STACK.md` §Resilience, Safety, and Governance — Spring Security baseline guidance.
- `.planning/research/STACK.md` §Testing and Quality — Testcontainers and integration test expectations.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java` — Canonical event envelope for persistence and SSE.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventPayload.java` and `RunEventType.java` — Strongly typed payload hierarchy and stable wire-name taxonomy for REST/SSE mapping.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/AgentRuntime.java` — Runtime port with `start(RunContext)` and `cancel(String runId, String reason)`.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/runtime/RunContext.java`, `Run.java`, `RunStatus.java`, `RunInput.java`, `CancellationToken.java` — Core runtime state/input/cancellation contracts used by App orchestration.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/session/Session.java`, `SessionEntry.java`, `SessionContextResolver.java` — Append-only session tree contracts for Session-centric API and history/read models.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/common/PlatformIds.java` — Typed IDs for tenant/user/session/run/workspace/trace/correlation/causation context.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventSink.java` — Single publish port to implement persist-then-emit fanout.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Synchronous fake `AgentRuntime` implementation for no-key E2E.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/EventCollector.java`, `FakeModelClient.java`, `FakeToolInvoker.java`, `FakePolicy.java`, `DeterministicIds.java`, `DeterministicClock.java` — Deterministic fake runtime/testing assets for Phase 2 E2E.

### Established Patterns
- Domain uses Java records, sealed interfaces, compact validation, and no framework annotations.
- COLA boundaries are enforced by ArchUnit: Domain cannot depend on Spring/Jackson/Jakarta/DB/Vaadin/PF4J/MCP/provider SDKs or outer packages; App is also currently restricted to Java/App/Domain/Client dependencies.
- Existing modules are ready for Phase 2 filling: `pi-agent-client`, `pi-agent-app`, `pi-agent-infrastructure`, and `pi-agent-adapter-web` exist but are mostly empty shells.
- Event ordering is a contract: fake runtime and tests expect per-run monotonic sequence and exactly one terminal event as the last observable event.
- Maven currently uses Java 21 settings but the local Maven launcher may default to Java 17; verification should use `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn test` where needed.

### Integration Points
- `pi-agent-adapter-web` should host Spring Boot REST/SSE controllers, security adapter/config, and client DTO mapping.
- `pi-agent-app` should define use-case services plus persistence/query/queue ports for create-run, cancel-run, get session/run status/detail, list events, and replay event history.
- `pi-agent-infrastructure` should implement App ports with PostgreSQL/JDBC/Flyway, EventSink persistence, DB-backed queue/worker, and read model queries.
- `pi-testkit` fake runtime should drive headless API/SSE E2E through Spring Boot tests and Testcontainers PostgreSQL.

</code_context>

<specifics>
## Specific Ideas

- User explicitly prefers “抽象好队列，实现可以先用 DB；其他的也一样，抽象好做好拓展性”: downstream planning should design queue/scheduler and similar infrastructure boundaries as abstractions first, while using a simple DB-backed implementation for Phase 2.
- The API should be Session-centric even though run IDs remain first-class for event ordering, cancellation, and traceability.
- Phase 2 should be checkpoint-ready, not crash-resumable: reserve evolution points without implementing durable replay/recovery.

</specifics>

<deferred>
## Deferred Ideas

- Full crash-resumable durable execution and replay workers — deferred beyond Phase 2 unless roadmap explicitly promotes v2 ADV-02.
- Real model provider/OpenAI-compatible streaming — Phase 3.
- Governed ToolExecutionGateway, policy/audit redaction depth, and real tool execution — Phase 4.
- Web Console/Admin Governance UI — Phase 5.
- Redis/Kafka/Rabbit/external queue implementation — future scaling option behind the Phase 2 queue abstraction.
- Full OAuth/RBAC/multi-tenant authorization depth — later hardening; Phase 2 is dev auth + JWT-ready with IDs consistently present.

</deferred>

---

*Phase: 02-cloud-server-persistence-sse-and-baseline-security*
*Context gathered: 2026-06-14*
