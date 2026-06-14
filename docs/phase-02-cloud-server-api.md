# Phase 2 Cloud Server API Contract

Phase 2 exposes the Java Agent Runtime through a session-centric Spring Boot Cloud Server API. This contract is intended for downstream Agent Web Console, Admin Governance, future TUI, and CLI clients. It is provider-neutral, no-key testable, and uses the same persisted `RunEventDto` envelope for REST event history and SSE streaming.

## Endpoint Index

All `/api/**` endpoints require the Phase 2 security context. In `dev`/`test`, safe development headers can provide tenant/user placeholders; production is JWT-resource-server ready.

| Method | Endpoint | Purpose |
| --- | --- | --- |
| `POST` | `/api/sessions` | Create a session in a workspace. |
| `GET` | `/api/sessions/{sessionId}` | Fetch session metadata. |
| `GET` | `/api/sessions/{sessionId}/history` | Fetch session history/read model. |
| `POST` | `/api/sessions/{sessionId}/runs` | Create and enqueue an Agent Run; automatic worker activation is triggered by product wiring. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}` | Fetch run detail. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/status` | Fetch current run status and terminal flag. |
| `POST` | `/api/sessions/{sessionId}/runs/{runId}/cancel` | Request cancellation; queued runs can be terminally cancelled immediately, running runs observe cancellation through runtime/worker state. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/events?afterSequence=0&limit=500` | Fetch persisted run events after a run-scoped sequence cursor. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/steps` | Fetch step read model. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/messages` | Fetch run messages. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/tool-calls` | Fetch tool-call read model rows. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/result` | Fetch terminal result/failure payloads. |
| `GET` | `/api/sessions/{sessionId}/runs/{runId}/stream` | SSE stream with persisted replay before live subscription; supports `afterSequence` and `Last-Event-ID`. |

## DTO Contract Summary

### Session and Run Commands

- `CreateSessionRequest`: `workspaceId`, `metadata`.
- `CreateRunRequest`: `agentId`, `inputType`, `input`, `workspaceId`, `metadata`.
- `CancelRunRequest`: `reason`.

### Run Status and Results

- Run status values exposed by Phase 2 include `QUEUED`, `RUNNING`, `CANCELLING`, `COMPLETED`, `FAILED`, `CANCELLED`, `TIMED_OUT`, and `POLICY_BLOCKED` where applicable.
- Terminal event wire names are exactly `run.completed`, `run.failed`, `run.cancelled`, and `run.policy_blocked`.
- Timeout currently reports run status `TIMED_OUT` and may pair with a persisted terminal `run.failed` event payload describing timeout failure.

## Run Event DTO Fields

REST history and SSE stream payloads use the same `RunEventDto` fields exactly:

- `eventId`
- `tenantId`
- `userId`
- `sessionId`
- `runId`
- `stepId`
- `workspaceId`
- `sequence`
- `timestamp`
- `type`
- `traceId`
- `correlationId`
- `causationId`
- `visibility`
- `redaction`
- `payloadSchema`
- `payloadVersion`
- `payload`

## SSE Replay Contract

- SSE endpoint: `GET /api/sessions/{sessionId}/runs/{runId}/stream`.
- Canonical ordering is run-scoped monotonic `sequence`.
- SSE event `id` is the decimal run event `sequence`.
- Reconnect clients may send `Last-Event-ID`; when positive and parseable it overrides `afterSequence`.
- Server performs durable replay before live fanout subscription, so reconnects receive events with `sequence > cursor`.

## Persistence and Worker Contract

- PostgreSQL/Flyway owns durable `sessions`, `runs`, `run_events`, `steps`, `messages`, `tool_calls`, `audit_records`, and `run_queue` tables.
- The product path for run execution is REST create-run → App use case → DB queue → `RunWorkerScheduler.triggerAsync()`/scheduled polling → dispatcher → `AgentRuntime` → persist-then-emit event sink.
- Tests must not call dispatcher internals directly for end-to-end product behavior.

## Phase 2 Non-Goals

Phase 2 intentionally has no real model provider, no ToolExecutionGateway, no UI, no MCP/PF4J/plugins, no Redis/Kafka/Rabbit, no crash-resumable replay, and no unrestricted shell/file tools.

Deferred work is tracked in later roadmap phases: model providers in Phase 3, governed tool/workspace execution in Phase 4, Agent Web Console/Admin UI in Phase 5, Java extension surfaces in Phase 6, MCP in Phase 7, dynamic plugin JARs in Phase 8, and production observability hardening in Phase 9.
