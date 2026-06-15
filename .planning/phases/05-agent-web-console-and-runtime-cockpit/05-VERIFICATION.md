---
phase: 05-agent-web-console-and-runtime-cockpit
verified: 2026-06-15T11:21:00Z
status: human_needed
score: 8/8 must-haves verified
human_verification:
  - test: "Visual UX pass for Console and Admin Governance"
    expected: "Three-column Console, catalog cards, event/tool/approval cards, and admin governance views are readable, navigable, and acceptable for the product experience."
    why_human: "Automated checks validate routes, data, API wiring, and Playwright behavior, but final visual quality and usability judgment is subjective."
---

# Phase 5: Agent Web Console and Runtime Cockpit Verification Report

**Phase Goal:** Agent Web Console and Runtime Cockpit — Cloud Server + Agent Web Console with Agent Catalog, Chat/Run entry, session/run management, SSE/event display, tool execution visibility, approval surfaces, and inspect-only Admin Governance.
**Verified:** 2026-06-15T11:21:00Z
**Status:** human_needed
**Re-verification:** No — initial verification

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | User can browse an Agent Catalog and enter Chat for a selected/default Agent. | ✓ VERIFIED | `AgentCatalogController` exposes `GET /api/agents`; `DefaultAgentCatalogQueryService` returns `cloud-general-agent` with name, description, capabilities, model ref, allowed tools/scopes, risks, side effects, and entry actions; `AgentCatalogPanel`/`AgentCard` consume `ConsoleHttpClient.agentCatalogPath()`; Playwright happy path asserts `/api/agents`. |
| 2 | User can send chat input, receive streaming/event output, and see Run status/timeline. | ✓ VERIFIED | `ConsoleView.planChatSubmission()` creates/reuses session plan, builds `CreateRunRequest(inputType="chat")`, and subscribes through `EventStreamClient`; `RunEventStreamController` serves `/api/sessions/{sessionId}/runs/{runId}/stream`; `RunEventRenderer` maps model/status/terminal events into the chat narrative; Playwright happy path asserts model deltas and terminal result. |
| 3 | User can see tool calls as cards with status, risk/side-effect label, progress, redacted summary, and errors. | ✓ VERIFIED | `RunEventRenderer` routes `payloadSchema=tool.lifecycle` to `ToolCallCard`; `ToolCallCard` renders tool name, status, purpose, risk, side effect, progress, result, error, details, and redacts sensitive strings; focused tests and Playwright assert tool lifecycle events. |
| 4 | User can view/continue Session history and cancel a running Run. | ✓ VERIFIED | `ConsoleHttpClient` exposes session history, run creation, event history, and cancel paths; `ConsoleView.selectSession()` and `planCancelRunningRun()` preserve session/run context; Playwright continuation/cancellation branch passes. |
| 5 | User/Admin can approve or reject gated tool calls through approval cards backed by backend API. | ✓ VERIFIED | `ApprovalController` exposes list and decision endpoints tied to session/run/approval ID; `DefaultApprovalService` reads approval-required events, records audit, publishes decision/reject events; `ApprovalCard`, `ApprovalPanel`, and `AdminApprovalQueueView` call `ConsoleHttpClient.approvalDecisionPath()` with original IDs; Playwright approve/reject branch passes. |
| 6 | Admin can inspect provider, extension, MCP, plugin, tool registry, policy decision, and audit governance views. | ✓ VERIFIED | `AdminGovernanceController` exposes read-only overview/policy/audit APIs; `DefaultGovernanceQueryService` returns runtime/provider/tool statuses plus extension/MCP/plugin placeholder statuses; `AdminGovernanceOverviewView`, `AdminRegistryStatusView`, `AdminPolicyDecisionsView`, and `AdminAuditView` render inspect-only views; Playwright governance branch passes. |
| 7 | Web GUI uses public REST/SSE/read-model APIs rather than private runtime or database access. | ✓ VERIFIED | UI package imports scan found no App/Domain/repository/persistence imports; UI uses `ConsoleHttpClient`, `EventStreamClient`, and `pi-agent-client` DTOs. Public API controllers delegate to App query/command services. |
| 8 | Browser E2E validates Phase 5 with no real model keys/Docker dependency. | ✓ VERIFIED | `playwright.config.ts` runs `scripts/e2e-web-server.sh` with `test,e2e` fake runtime profile; `npm run e2e -- --list` found 4 Chromium tests; `npm run e2e` passed 4/4. |

**Score:** 8/8 truths verified

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `pi-agent-adapter-web/pom.xml` | Adapter-only Vaadin dependencies | ✓ VERIFIED | Focused Phase 5 Maven tests compile/run adapter web; no evidence of Vaadin in Domain/App from UI import scan. |
| `ConsoleHttpClient.java` | Public REST client boundary | ✓ VERIFIED | Provides `/api` paths for sessions/runs/events/cancel, agents, tools, approvals, admin governance and references `pi-agent-client` DTOs only. |
| `EventStreamClient.java` | Public SSE stream helper | ✓ VERIFIED | Builds `/api/sessions/{sessionId}/runs/{runId}/stream?afterSequence=N`, matching `RunEventStreamController`. |
| `AgentCatalogItemDto.java` / `AgentCatalogController.java` | Public Agent Catalog contract/API | ✓ VERIFIED | DTO exists; controller is thin `GET /api/agents` delegation to `AgentCatalogQueryService`; no mutation mappings found. |
| `ApprovalDecisionRequest.java`, `DefaultApprovalService.java`, `ApprovalController.java` | Approval contracts/service/API | ✓ VERIFIED | Approval decisions carry actor role, original session/run/approval/tool context; service records audit and publishes events. |
| `GovernanceOverviewResponse.java`, `AdminGovernanceController.java` | Admin governance contract/API | ✓ VERIFIED | Overview/policy/audit endpoints are GET-only and backed by `GovernanceQueryService`. |
| `ConsoleView.java`, `ChatEventStreamPanel.java` | Chat-first three-column Console and event stream | ✓ VERIFIED | Route `console`; three-column workbench; chat input, session continuation, SSE subscription plan, and cancellation plan. |
| `AgentCatalogPanel.java`, `ToolCallCard.java` | Catalog cards and tool lifecycle cards | ✓ VERIFIED | Catalog panel uses `/api/agents`; tool cards render `tool.lifecycle` events with redaction. |
| `ApprovalCard.java`, `AdminApprovalQueueView.java` | User/Admin approval surfaces | ✓ VERIFIED | Approval cards include context and actions; decision plans call backend endpoint with original session/run/approval IDs. |
| `AdminGovernanceOverviewView.java`, `AdminRegistryStatusView.java`, `AdminPolicyDecisionsView.java`, `AdminAuditView.java` | Inspect-only admin views | ✓ VERIFIED | Render runtime/provider/tool/extension/MCP/plugin, policy, and audit summaries; mutation/search/filter/export controls intentionally absent. |
| `playwright.config.ts`, `e2e/phase-05-web-console.spec.ts` | Browser E2E harness/spec | ✓ VERIFIED | Lists 4 Phase 5 Chromium tests and full `npm run e2e` passes. |
| `docs/phase-05-web-console.md` | Phase 5 contract documentation | ✓ VERIFIED | Documents API/UI contracts, security/redaction boundaries, E2E commands, and deferred EXT/MCP/PLUG/OPS scope. |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| Vaadin views | `/api/**` and run SSE | `ConsoleHttpClient` / `EventStreamClient` | ✓ WIRED | UI imports scan found no private App/Domain/repository access; helpers build public API paths. |
| `AgentCatalogController` | `AgentCatalogQueryService` | `agentCatalogQueryService.listAgents(...)` | ✓ WIRED | Thin controller delegates with `RequestContext`; no POST/PUT/PATCH/DELETE mappings. |
| `ApprovalController` | `DefaultApprovalService` through query/command interfaces | `listPendingApprovals(...)` / `decide(...)` | ✓ WIRED | Controller routes include original session/run/approval IDs and service reads persisted run events/audit/event sink. |
| `AdminGovernanceController` | `GovernanceQueryService` | `overview`, `policyDecisions`, `audits` | ✓ WIRED | GET-only controller delegates to read-only query service. |
| `ConsoleView` | `ConsoleHttpClient` / `EventStreamClient` | session/run/history/cancel paths and SSE stream spec | ✓ WIRED | Focused tests assert create session/run, continuation, history, cancel, and stream URL plans. |
| `RunEventRenderer` | `ToolCallCard` / `ApprovalCard` | `tool.lifecycle` and approval-required events | ✓ WIRED | Renderer instantiates cards for tool lifecycle and approval events; tests assert rendered component types. |
| Playwright tests | Spring Boot Vaadin app | `webServer` / `baseURL` / fake runtime profile | ✓ WIRED | `playwright.config.ts` starts e2e web server; `npm run e2e` passed 4/4. |

### Data-Flow Trace (Level 4)

| Artifact | Data Variable | Source | Produces Real Data | Status |
|----------|---------------|--------|--------------------|--------|
| `AgentCatalogPanel` | `AgentCatalogResponse.agents` | `GET /api/agents` → `AgentCatalogController` → `DefaultAgentCatalogQueryService` | Yes — default runnable Agent metadata returned through App query service | ✓ FLOWING |
| `ConsoleView` / `ChatEventStreamPanel` | chat/run events | Public session/run APIs and `EventStreamClient` SSE path; Playwright creates real fake-runtime runs and event histories | Yes — Playwright observes model/tool/run events and terminal results | ✓ FLOWING |
| `ToolCallCard` | `RunEventDto.payload` | persisted/public run event DTOs with `payloadSchema=tool.lifecycle` | Yes — Playwright and focused tests assert tool lifecycle payloads | ✓ FLOWING |
| `ApprovalCard` / `AdminApprovalQueueView` | `ApprovalSummaryDto` and decision request | `ApprovalController` → `DefaultApprovalService` → `RunEventStore` approval-required events; decisions to audit/event sink | Yes — approve/reject branches pass in Playwright | ✓ FLOWING |
| Admin governance views | `GovernanceOverviewResponse`, `PolicyDecisionSummaryDto`, `AuditSummaryDto` | `AdminGovernanceController` → `DefaultGovernanceQueryService` | Partial by design — runtime/provider/tool status real; extension/MCP/plugin placeholders; policy/audit lists may be empty but valid read models | ✓ FLOWING |

### Behavioral Spot-Checks

| Behavior | Command | Result | Status |
|----------|---------|--------|--------|
| Focused Phase 5 Java controller/UI component tests pass | `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=WebConsoleFoundationTest,AgentCatalogControllerTest,ApprovalControllerTest,AdminGovernanceControllerTest,WebConsoleUserFlowTest,WebConsoleCatalogAndToolCardsTest,WebConsoleApprovalCardsTest,AdminGovernanceViewsTest test` | Command completed successfully in this environment. Docker/Testcontainers not required for this focused gate. | ✓ PASS |
| Phase 5 Playwright tests are discoverable | `npm run e2e -- --list` | 4 Chromium tests listed in `e2e/phase-05-web-console.spec.ts`. | ✓ PASS |
| Browser E2E passes with fake/no-key runtime | `npm run e2e` | 4 passed in 21.1s. | ✓ PASS |

### Requirements Coverage

| Requirement | Source Plan | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| GUI-01 | 05-02, 05-06, 05-09 | Agent Catalog listing with metadata and entry actions | ✓ SATISFIED | `/api/agents`, `DefaultAgentCatalogQueryService`, catalog cards, Playwright happy path. |
| GUI-02 | 05-05, 05-09 | Chat-style interaction page, streaming model output, run status; API open to non-chat modes | ✓ SATISFIED | `ConsoleView`, `EventStreamClient`, `RunEventRenderer`, `CreateRunRequest(inputType="chat")`, catalog includes task mode, Playwright model deltas. |
| GUI-03 | 05-06, 05-09 | Tool execution cards with status/risk/progress/redacted result/errors | ✓ SATISFIED | `ToolCallCard`, `RunEventRenderer`, focused tests, Playwright tool event assertions. |
| GUI-04 | 05-05, 05-09 | Session history and continuation | ✓ SATISFIED | `ConsoleHttpClient.sessionHistoryPath`, `ConsoleView.selectSession`, Playwright continuation branch. |
| GUI-05 | 05-05, 05-09 | Cancel running run from Web Console | ✓ SATISFIED | `ConsoleView.planCancelRunningRun`, `ConsoleHttpClient.cancelRunPath`, Playwright cancellation branch. |
| GUI-06 | 05-03, 05-07, 05-09 | User/Admin approve or reject gated tool calls | ✓ SATISFIED | Approval DTO/API/service, `ApprovalCard`, `AdminApprovalQueueView`, Playwright approve/reject branch. |
| GUI-07 | 05-04, 05-08, 05-09 | Inspect runtime governance views | ✓ SATISFIED | Admin governance API/views include runtime/providers/tools/extension/MCP/plugin placeholders, policy, audit; Playwright branch passes. |
| GUI-08 | 05-01..05-09 | GUI uses public REST/SSE/read-model APIs only | ✓ SATISFIED | UI imports scan found no App/Domain/repository access; helpers and client DTOs are the boundary. |
| E2E-07 | 05-09 | Browser E2E validates catalog, interaction, streaming, tool cards, approvals, history, cancel, governance | ✓ SATISFIED | `npm run e2e -- --list` lists 4 tests; `npm run e2e` passes 4/4. |

No orphaned Phase 5 requirement IDs were found in `.planning/REQUIREMENTS.md`: GUI-01 through GUI-08 and E2E-07 are all claimed by Phase 5 plans and covered above.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `ChatEventStreamPanel.java` | 15/27/43 | `PLACEHOLDER` constant used as Vaadin input placeholder | ℹ️ Info | Not a stub; it is user-facing input hint text and does not replace implementation. |
| `docs/phase-05-web-console.md` | 32 | Documents SSE as `/events/stream` while implemented route/helper use `/stream` | ⚠️ Warning | Documentation inconsistency only; actual code and tests use working `/api/sessions/{sessionId}/runs/{runId}/stream`. |

### Human Verification Required

### 1. Visual UX pass for Console and Admin Governance

**Test:** Open `/console`, interact with catalog/chat/tool/approval/session/cancel flows, then open `/admin/governance/*` views.
**Expected:** Layout is visually readable, navigation is understandable, cards are comprehensible, and inspect-only boundaries are clear.
**Why human:** Automated unit and Playwright tests verify behavior and data wiring, but final visual quality/usability is subjective.

### Gaps Summary

No goal-blocking gaps found. All Phase 5 observable truths, required artifacts, key links, requirement IDs, and focused/browser behavioral checks are verified. Status is `human_needed` only for final subjective visual UX review.

---

_Verified: 2026-06-15T11:21:00Z_
_Verifier: the agent (gsd-verifier)_
