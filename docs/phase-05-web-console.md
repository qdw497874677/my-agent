# Phase 05 Agent Web Console and Runtime Cockpit Contracts

Phase 05 delivers the Java-first Agent Web Console and inspect-only Admin Governance surface. The UI is backed by public REST/SSE/read-model APIs rather than Domain, App, persistence, or runtime internals.

## Requirement Coverage

| Requirement | Phase 05 validation |
|-------------|---------------------|
| `GUI-01` | `GET /api/agents` and Console catalog cards expose Agent name, description, capabilities, model ref, allowed tools/scopes, risk labels, side-effect labels, and entry actions. |
| `GUI-02` | `/console` is the Chat-first entry point; run creation uses public session/run APIs and run progress is represented by ordered event history/SSE contracts. |
| `GUI-03` | Governed `tool.lifecycle` events render as expandable tool cards with tool id, status, policy decision, provenance, risk/side-effect metadata, redacted input/output summaries, preview, and error data. |
| `GUI-04` | Session selection and continuation are modeled through public session history and create-run paths, preserving past run/event visibility. |
| `GUI-05` | Console cancellation plans call `POST /api/sessions/{sessionId}/runs/{runId}/cancel` and show cancelling/terminal status feedback. |
| `GUI-06` | User and Admin approval cards call `GET /approvals` and `POST /approvals/{approvalId}/decision` with explicit `APPROVE`/`REJECT` decisions and actor roles. |
| `GUI-07` | Admin Governance exposes read-only overview, policy decision, audit, registry/extension, MCP, and plugin status surfaces. Extension/MCP/plugin details remain status placeholders for later phases. |
| `GUI-08` | Vaadin components consume `ConsoleHttpClient`, `EventStreamClient`, and `pi-agent-client` DTOs only; browser E2E validates public API behavior. |
| `E2E-07` | `npm run e2e` runs Playwright against a no-key Spring Boot test server covering catalog, run streaming/events, tool lifecycle, approvals, session continuation, cancellation, and governance APIs. |

## Public API and UI Contract Index

### Agent Catalog

- API: `GET /api/agents`
- DTO: `AgentCatalogResponse` with `AgentCatalogItemDto` records.
- UI: `AgentCatalogPanel` / `AgentCard` render catalog metadata and entry action hints.
- Contract: Agent Catalog is read-only in Phase 5. Agent Studio create/edit/publish is deferred.

### Chat, Runs, Events, and SSE

- Session API: `POST /api/sessions`, `GET /api/sessions/{sessionId}/history`.
- Run API: `POST /api/sessions/{sessionId}/runs`, `GET /status`, `GET /events`, `GET /result`, `POST /cancel`.
- SSE API: `GET /api/sessions/{sessionId}/runs/{runId}/events/stream` with replay-before-subscribe semantics inherited from Phase 2.
- UI boundary: `ConsoleView` builds action plans; transport remains outside components through `ConsoleHttpClient` and `EventStreamClient`.
- Event mapping: model deltas, run lifecycle, policy/tool lifecycle, approval decisions, and terminal events are rendered into one runtime narrative.

### Tool and Approval Cards

- Tool cards consume public `RunEventDto` entries where `payloadSchema=tool.lifecycle`.
- Tool card payloads are redacted summaries only: no raw secrets, unbounded arguments, or raw tool output are required for display.
- Approval list API: `GET /api/sessions/{sessionId}/runs/{runId}/approvals`.
- Approval decision API: `POST /api/sessions/{sessionId}/runs/{runId}/approvals/{approvalId}/decision`.
- Approval IDs are derived from preview IDs with tool-call ID fallback. `APPROVE` records an approval decision; `REJECT` records a same-run policy-blocked outcome.

### Admin Governance

- Overview: `GET /api/admin/governance/overview`.
- Policy decisions: `GET /api/admin/governance/policy-decisions`.
- Audit summaries: `GET /api/admin/governance/audits`.
- Vaadin routes are inspect-only and do not expose mutations for provider/tool/policy/plugin/MCP/extension configuration in Phase 5.
- Extension, MCP, and plugin panels intentionally show `FUTURE_ENABLED`/`UNCONFIGURED` style status metadata until Phases 6, 7, and 8 deliver real integrations.

## Browser E2E Commands

```bash
npm run e2e:install -- --with-deps=false
npm run e2e -- --list
npm run e2e
```

The Playwright web server runs Spring Boot with `test,e2e` profiles, in-memory stores, a deterministic fake runtime, no real model keys, no Docker, and no external services. Screenshots, videos, and traces are retained on failure.

## Security and Redaction Boundaries

- `/api/**` remains authenticated; test/dev profile authentication uses safe dev headers only.
- Vaadin UI routes are public shell routes, while data comes from authenticated REST/SSE APIs.
- Web Console and Admin Governance consume public DTOs with redacted summaries.
- Secrets, raw provider credentials, unrestricted host filesystem paths, unrestricted shell commands, plugin mutation controls, and MCP server configuration are out of scope for Phase 5.

## Explicit Deferrals

- SPI/Spring extension discovery and health details: Phase 6.
- Remote MCP server configuration, discovery, and execution: Phase 7.
- Dynamic plugin JAR lifecycle and controls: Phase 8.
- Production observability/metrics/OpenTelemetry hardening: Phase 9.
- Full Agent Studio, visual workflow builder, and TUI/CLI clients remain future scope.
