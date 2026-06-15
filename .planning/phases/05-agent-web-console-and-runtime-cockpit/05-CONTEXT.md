# Phase 5: Agent Web Console and Runtime Cockpit - Context

**Gathered:** 2026-06-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 5 delivers the all-Java Agent Web Console and Runtime Cockpit using Vaadin Flow. It must provide a user-facing Agent interaction surface with Agent Catalog access, Chat/Run entry, streaming execution visibility, tool-call cards, approval cards, Session history/continuation, cancellation, and a separate basic Admin Governance surface for runtime health and read-only governance visibility.

This phase must use public REST/SSE/read-model APIs rather than private runtime objects or direct database access. It does **not** implement full Agent Studio/agent editing, configuration management, extension loading, MCP setup, dynamic plugin management, full audit search/export, visual workflow building, or a TUI/CLI client.

</domain>

<decisions>
## Implementation Decisions

### Console Information Architecture
- **D-01:** The user-facing Console should be **Chat-first**, not Catalog-first. Opening the User Console should emphasize entering or continuing an Agent interaction, with Catalog available as the way to choose/switch Agents.
- **D-02:** Agent Catalog cards must show enough **run-decision information** for users to choose an Agent: name, description, supported input modes/capabilities, model/provider reference where safe, allowed tools/tool scopes, risk/side-effect indicators, and clear entry actions.
- **D-03:** User Console and Admin Governance should be **two separate UI surfaces/routes/layouts**, not one combined navigation. They may live in the same Vaadin application/module, but downstream planning should treat them as distinct product areas with distinct navigation and authorization.
- **D-04:** Phase 5 must add or expose a backend **read-only Agent Catalog API** such as `/api/agents` or equivalent read-model API. Vaadin must consume that public API/read model; static in-UI Agent definitions are not acceptable for the final implementation.

### Run Cockpit Experience
- **D-05:** Run Cockpit should use a **three-column workbench** layout: left column for recent Sessions/history, center column for the Chat/event stream and input, and right column for run/tool/detail context where appropriate.
- **D-06:** Streaming model output and runtime/tool/policy events should all appear in the **chat/event stream**, rather than hiding technical events in a separate-only timeline. The UI may visually distinguish message text, run events, tool cards, policy events, and status changes, but the user should see one integrated execution narrative.
- **D-07:** Session history should support a left-side recent Sessions list and **continue-session** flow. Selecting a Session loads prior messages, tool calls, terminal results, and related run history, then lets the user create a new Run in that Session.
- **D-08:** Running Runs must expose a prominent **Cancel** action with clear status feedback. After cancellation is requested, the stream/workbench should show cancelling/cancelled state transitions and any terminal event/result returned by the public API.

### Tool Cards and Approval Flow
- **D-09:** Tool calls should render as **summary cards with expandable details** inside the integrated chat/event stream. The default card must show tool name, status, purpose/summary, risk/side-effect label, progress where available, redacted result summary, and errors. Expanded details may show the event sequence and additional redacted diagnostics.
- **D-10:** Approval cards must show enough context for a safe decision: tool name, policy reason, risk/side-effect label, provision/impact preview, redacted argument summary, expected consequence, and clear Approve/Reject actions.
- **D-11:** Approve/Reject must apply to the **original waiting Run/tool call**. Approve should resume the suspended/waiting execution path; Reject should produce the appropriate rejected/policy terminal or resumable outcome in the same Run/session timeline. Planning must include the missing approval action/resume API and not fake approval as UI-only state.
- **D-12:** Both the initiating User and an Admin may approve/reject gated tool calls in Phase 5. Implementation should preserve a role/principal-aware authorization seam; dev/test auth may simulate both paths.

### Admin Governance Views
- **D-13:** Admin Governance should open with **runtime health plus registry/governance overview**: provider status, tool registry health, extension/MCP/plugin placeholders, policy decision visibility, audit visibility, and links into read-only detail views.
- **D-14:** Extension, MCP, and plugin areas should appear as **read-only placeholder/status views** in Phase 5 because their active implementations belong to Phases 6, 7, and 8. Show empty/unconfigured/future-enabled states; do not implement configuration, loading, enable/disable, or server setup in this phase.
- **D-15:** Policy decisions and audit records should be presented as **recent read-only lists** with redacted summaries and links/context back to run/session/tool where available. Full search/filter/export is deferred.
- **D-16:** Phase 5 Admin Governance is **inspect-only**. Do not allow provider/tool/policy/plugin/MCP/extension configuration mutation from the UI in this phase.

### Browser E2E Verification
- **D-17:** Browser E2E should use **Playwright** for Phase 5, even though it introduces a Node-based test tool, because it best fits modern browser/SSE validation. Java production code remains Java-first; Playwright is test-only.
- **D-18:** E2E coverage should include **one complete happy path plus key branches**: Agent entry/catalog, Chat/Run creation, streaming output, integrated event stream, tool card, approval card approve/reject path, Session history/continuation, cancellation, and basic Admin read-only governance views.
- **D-19:** Browser E2E must run against the existing **no-key fake runtime** style: fake model, fake tools, fake approval/policy branches, and deterministic test data. It must not require real model keys or external services for default verification.
- **D-20:** UI verification should be **behavior-first with a small number of screenshots** as debug/artifact output for key pages or failures. Full visual regression baselines are not required in Phase 5.

### Folded Todos
- No pending todos matched Phase 5 scope.

### the agent's Discretion
- Exact Vaadin component classes, route names, responsive breakpoints, styling tokens, and detailed visual treatment are planner/designer discretion as long as the locked product decisions above hold.
- Exact public endpoint names for Agent Catalog, approval action/resume, admin read models, policy decisions, and audit summaries are planner discretion, but they must be public REST/SSE/read-model APIs and must preserve `pi-agent-client` DTO boundaries.
- Exact UI empty-state copy, loading skeletons, iconography, and event color taxonomy are flexible, but must avoid exposing raw secrets or sensitive payloads.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 5 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 5 — Phase goal, GUI-01..GUI-08 and E2E-07 mapping, success criteria, UI hint, and research-needed topics.
- `.planning/REQUIREMENTS.md` §Agent Web Console and Admin Governance — GUI-01 through GUI-08 details for Catalog, interaction page, tool cards, Session history, cancellation, approvals, Admin Governance, and public API-only UI access.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E-07 browser E2E requirement for Catalog, interaction page, streaming output, tool cards, approval cards, session history, cancellation, and governance views.
- `.planning/PROJECT.md` §Current State — Phase 5 may build tool cards/approval views on validated `/api/tools` and `tool.lifecycle` contracts.
- `.planning/STATE.md` — Current Phase 5 focus and open E2E tool-stack question.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — RunEvent envelope, generic runtime input, Session tree, framework-free Domain, and public/client boundary decisions.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Session-centric REST API, `pi-agent-client` DTO ownership, persist-then-emit SSE, replay semantics, dev auth + JWT-ready security, and public API/SSE mapping decisions.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — Provider registry shape, provider status visibility for Admin Governance, secret redaction, and no-key fake provider E2E patterns.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Tool descriptor/gateway decisions, `REQUIRE_APPROVAL` suspend/wait semantics, tool lifecycle card state machine, `/api/tools`, redaction/audit requirements, and Phase 5 approval/tool-card deferrals.
- `docs/phase-01-domain-contracts.md` — Runtime, event, session, workspace, and testkit contract index.
- `docs/phase-02-cloud-server-api.md` — Cloud Server REST/SSE/session/run/event/audit API contract index.
- `docs/phase-03-model-provider-contracts.md` — Provider registry and OpenAI-compatible adapter contract index for Admin provider status context.
- `docs/phase-04-governed-tool-contracts.md` — Governed tool contracts, `/api/tools`, `tool.lifecycle`, approval-required behavior, redaction, audit, and Phase 5 consumption guidance.

### Architecture and Stack Guidance
- `.planning/research/STACK.md` §Admin GUI — Vaadin Flow guidance for all-Java Admin/Web Console.
- `.planning/research/STACK.md` §Cloud Server API Layer — Spring MVC REST/SSE guidance and JSON DTO boundary expectations.
- `.planning/research/STACK.md` §Testing and Quality — browser/integration testing expectations, Testcontainers/fake services, and no-key verification patterns.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/SessionController.java` — Existing public Session create/detail/history API for Session list and continuation.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunController.java` — Existing public Run create/status/detail/events/steps/messages/tool-calls/result/cancel APIs for Cockpit and cancellation.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/RunEventStreamController.java` — Existing SSE stream endpoint for live Run events.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/ToolRegistryController.java` — Existing read-only `/api/tools` endpoint for tool catalog/Admin tool views.
- `pi-agent-client` DTOs including `RunEventDto`, `CreateRunRequest`, `SessionResponse`, `SessionHistoryResponse`, `RunDetailResponse`, `RunStatusResponse`, `RunResultResponse`, `EventHistoryResponse`, `CancelRunRequest`, `ToolDescriptorDto`, and `ToolCatalogResponse` — public contracts Vaadin should consume or extend.
- `pi-agent-adapter-web/src/test/java/.../TestCloudRuntimeConfiguration.java` and existing Cloud Server E2E tests — no-key fake runtime patterns to reuse for browser E2E.

### Established Patterns
- Domain/App remain framework-free; Vaadin must live in Adapter Web or a dedicated adapter module and must not leak into Domain/App contracts.
- REST controllers are thin and delegate to App use cases; Vaadin should follow the same public API/read-model boundary rather than reaching into private runtime services.
- `pi-agent-client` owns external DTOs; public UI data contracts should be represented there or mapped through adapter DTOs without exposing Domain records directly.
- Persist-then-emit SSE and run-scoped sequence ordering are established. UI should handle durable event replay/reconnect semantics rather than assuming live-only events.
- Events, tool outputs, provider metadata, audit records, and Admin views must use redacted summaries by default.

### Integration Points
- Add Vaadin dependency/BOM and security route allowances in `pi-agent-adapter-web` (or a new web-console adapter module) while preserving API authentication behavior.
- Add public Agent Catalog read-model/API because `CreateRunRequest.agentId` exists but there is no catalog/list endpoint yet.
- Add public approval action/resume/reject API because Phase 4 emits approval-required/waiting semantics but does not yet provide a user action path.
- Add basic read-only Admin Governance read models/APIs for provider status, extension/MCP/plugin placeholder status, policy decisions, audit summaries, and registry health where existing backend contracts permit.
- Add Playwright browser E2E harness against the Spring Boot/Vaadin app using fake runtime/test data.

</code_context>

<specifics>
## Specific Ideas

- User explicitly chose a Chat-first user Console rather than Catalog-first or Dashboard-first.
- User explicitly chose a three-column Run Cockpit workbench.
- User wants all execution events visible in the chat/event stream, not hidden in a separate-only technical timeline.
- User wants User Console and Admin Governance separated as two UI surfaces.
- User wants Admin Governance to be read-only in Phase 5, with extension/MCP/plugin shown as placeholders until their later phases.
- User chose Playwright for browser E2E despite the otherwise Java-first stack; it is acceptable as test-only tooling.

</specifics>

<deferred>
## Deferred Ideas

- Agent creation/editing/publishing and full Agent Studio — out of scope for Phase 5.
- Extension configuration, enable/disable, and registration UX — Phase 6 or later.
- MCP server configuration/discovery management UX — Phase 7 or later.
- Dynamic plugin loading/disable/quarantine management UX — Phase 8 or later.
- Full audit search/filter/export and advanced governance operations — later Admin Governance hardening.
- Full visual regression test suite — deferred until UI stabilizes beyond Phase 5.
- TUI/CLI client experience — future client phases; must consume the same public APIs.

</deferred>

---

*Phase: 05-agent-web-console-and-runtime-cockpit*
*Context gathered: 2026-06-15*
