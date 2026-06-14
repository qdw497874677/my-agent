# Phase 2: Cloud Server, Persistence, SSE, and Baseline Security - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-14
**Phase:** 02-Cloud Server, Persistence, SSE, and Baseline Security
**Areas discussed:** Durability depth, API contract shape, SSE replay behavior, Security baseline, Persistence model, Execution queue and scheduling

---

## Gray Areas Selected

| Area | Description | Selected |
|------|-------------|----------|
| Durability depth | Whether Phase 2 locks to persisted history/query/cancel vs checkpoint-ready vs crash-resumable replay. | ✓ |
| API contract shape | REST resource shape and client DTO boundary for create/query/cancel/event APIs. | ✓ |
| SSE replay behavior | Persist-before-emit, replay cursor, and Last-Event-ID/sequence semantics. | ✓ |
| Security baseline | Dev/test principal, JWT-ready production boundary, tenant/user/trace context. | ✓ |
| Persistence model | PostgreSQL/Flyway table strategy and placement of repository/query ports. | ✓ |

---

## Durability Depth

| Option | Description | Selected |
|--------|-------------|----------|
| History only | Persist run/session/event/read-model history and support query/cancel/SSE replay; no crash recovery of in-flight runs. | |
| Checkpoint-ready | History only plus reserved checkpoint/replay fields/interfaces; no actual recovery execution in Phase 2. | ✓ |
| Crash-resumable | Restore in-flight runs after process crash; expands Phase 2 toward v2 ADV-02. | |

**User's choice:** Checkpoint-ready.
**Notes:** Phase 2 should reserve evolution points for later recovery/replay but must not implement crash-resumable execution now.

---

## API Contract Shape

| Option | Description | Selected |
|--------|-------------|----------|
| Run-centric | `/api/runs` is primary resource with create/get/status/cancel/events/history. | |
| Session-centric | `/api/sessions/{id}/runs` and nested session/run/history resources are primary. | ✓ |
| Hybrid | Support both run-centric and session-centric entry points early. | |

**User's choice:** Session-centric.
**Notes:** Run ID remains first-class for cancellation, event sequence, and correlation, but REST shape should emphasize sessions as the user-facing container.

### DTO Boundary

| Option | Description | Selected |
|--------|-------------|----------|
| Client DTOs | `pi-agent-client` owns external DTOs; adapter maps Domain/App to DTOs. | ✓ |
| Adapter DTOs | DTOs only live in `pi-agent-adapter-web`. | |
| Domain direct | Expose Domain records directly as JSON. | |

**User's choice:** Client DTOs.
**Notes:** Preserve Domain serialization neutrality and future CLI/TUI/Web Console reuse.

---

## SSE Replay Behavior

### Publish Ordering

| Option | Description | Selected |
|--------|-------------|----------|
| Persist then emit | Persist `RunEvent` successfully before emitting to SSE subscribers. | ✓ |
| Emit then persist | Emit first, persist asynchronously afterward. | |
| Async fanout | Persist and SSE are both asynchronous fanout paths. | |

**User's choice:** Persist then emit.
**Notes:** Prioritize durable ordering/reconnect correctness over lower latency.

### Replay Cursor

| Option | Description | Selected |
|--------|-------------|----------|
| Run sequence | Use per-run monotonic sequence as canonical replay cursor; map Last-Event-ID where practical. | ✓ |
| Event ID only | Use eventId/Last-Event-ID only. | |
| Both required | Require both eventId and sequence for reconnect. | |

**User's choice:** Run sequence.
**Notes:** Matches Phase 1 event ordering contract.

---

## Security Baseline

### Auth Level

| Option | Description | Selected |
|--------|-------------|----------|
| Dev auth + JWT-ready | Simple dev/test principal, production OAuth2 Resource Server/JWT-ready boundary. | ✓ |
| JWT mandatory | JWT required from Phase 2 for all calls. | |
| Placeholders only | No Spring Security, only IDs/correlation placeholders. | |

**User's choice:** Dev auth + JWT-ready.
**Notes:** Supports local deterministic E2E while preserving production security direction.

### Context Visibility

| Option | Description | Selected |
|--------|-------------|----------|
| Expose IDs safely | Expose tenant/user/session/run/trace/correlation IDs; hide tokens/claims/raw headers. | ✓ |
| Internal only | Keep IDs out of API/SSE responses. | |
| Full debug mode | Return full claims/header data in development. | |

**User's choice:** Expose IDs safely.
**Notes:** Debug/correlation IDs are public enough for API/SSE; sensitive auth material stays hidden.

---

## Persistence Model

### Port Placement

| Option | Description | Selected |
|--------|-------------|----------|
| App ports | `pi-agent-app` defines persistence/query ports; Infrastructure implements them. | ✓ |
| Domain ports | Domain owns repositories/event store ports. | |
| Infrastructure only | App calls Infrastructure directly without ports. | |

**User's choice:** App ports.
**Notes:** Keeps Domain clean and preserves COLA dependency direction.

### Schema Strategy

| Option | Description | Selected |
|--------|-------------|----------|
| Event log + read models | Append-only `run_events` plus query tables/read models for runs, sessions, steps, messages, tool calls, audit. | ✓ |
| Relational first | Relational tables are source of truth; events are derived logs. | |
| Event log only | Persist only events and reconstruct queries on demand. | |

**User's choice:** Event log + read models.
**Notes:** Balances event replay/history with efficient CLOUD-03 queries.

---

## Execution Queue and Scheduling

| Option | Description | Selected |
|--------|-------------|----------|
| In-process virtual threads | Submit fake/runtime loop to an in-process executor. | |
| Database queue | Abstract queue/scheduler boundary and use DB-backed queue/worker first. | ✓ |
| External queue | Introduce Kafka/Rabbit/Redis queue in Phase 2. | |

**User's choice:** User dismissed the menu and clarified: “抽象好队列，实现可以先用db。其他的也一样，抽象好做好拓展性”.
**Notes:** Captured as a broader architectural decision: define queue/scheduler abstractions first; default implementation can be DB-backed; apply the same abstraction-first/extensibility-first pattern to comparable infrastructure choices.

---

## the agent's Discretion

- Exact endpoint names and DTO record names are flexible under the Session-centric/client-DTO boundary.
- Exact checkpoint-ready fields are flexible as long as they do not imply implemented crash replay.
- Exact DB queue locking/polling design is flexible if it preserves testability and event/cancellation semantics.

## Deferred Ideas

- Crash-resumable durable execution/replay beyond checkpoint-ready fields.
- External queue systems such as Redis/Kafka/Rabbit in Phase 2.
- Full production RBAC/OAuth/multi-tenant authorization depth.
