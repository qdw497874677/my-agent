---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 02
status: executing
stopped_at: Completed 02-06-PLAN.md
last_updated: "2026-06-14T05:30:37.270Z"
progress:
  total_phases: 9
  completed_phases: 1
  total_plans: 18
  completed_plans: 11
---

# Project State: Pi Java Agent Platform

**Initialized:** 2026-06-13  
**Status:** Executing Phase 02
**Current Phase:** 02

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-13)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 02 — cloud-server-persistence-sse-and-baseline-security

## Workflow Configuration

- Mode: YOLO
- Granularity: Standard
- Parallelization: Enabled
- Research before phase planning: Enabled
- Plan checking: Enabled
- Phase verification: Enabled
- Planning docs committed to git: Yes

## Current Roadmap

| Phase | Name | Status |
|-------|------|--------|
| 1 | Runtime Spine, Workspace, and Domain Contracts | Complete |
| 2 | Cloud Server, Persistence, SSE, and Baseline Security | Pending |
| 3 | Model Provider Registry and OpenAI-Compatible Adapter | Pending |
| 4 | Governed Tool Registry and Invocation Pipeline | Pending |
| 5 | Agent Web Console and Runtime Cockpit | Pending |
| 6 | Java Extension Surface: SPI and Spring | Pending |
| 7 | MCP Client Bridge and Governed Remote Tools | Pending |
| 8 | Controlled Dynamic Plugin JARs | Pending |
| 9 | Observability, Policy, Tenancy, and Production Hardening | Pending |

## Phase 1 Summary

**Goal:** Establish a framework-independent Java Agent Runtime kernel and first-class Workspace contracts that all cloud, GUI, provider, tool, MCP, and plugin work will build on.

**Requirements:** CORE-01, CORE-02, CORE-03, CORE-04, CORE-05, CORE-06, CORE-07, CORE-08, CORE-09, WORK-01, WORK-02, WORK-04, WORK-05, OPS-04, OPS-06

**Key concerns for planning:**

- Keep runtime core independent of Spring Boot, Vaadin, PF4J, MCP, and provider SDKs.
- Use COLA boundaries: Adapter → App → Domain ← Infrastructure; Domain has zero outward dependencies.
- Keep runtime core generic: no Chat-only, Coding-only, single-provider, single-tool-protocol, or UI-driven assumptions.
- Define Workspace as a first-class domain concept instead of a thin wrapper over host filesystem.
- Define stable domain models before persistence/API/UI.
- Define event envelope carefully because REST/SSE/Admin/TUI/CLI/audit all depend on it.
- Use fake model/fake tool testkit to validate Agent loop without real providers.
- Use fake workspace and fake command executor so later E2E can validate file/command/resource actions without host shell dependencies.
- Design Phase 1 testkit so later E2E can validate Run lifecycle without real model keys.

## Research Summary

See `.planning/research/SUMMARY.md`.

Key findings:

- Build modular monolith first, distributed-capable later.
- Runtime core should be framework-independent; Spring, Spring AI, MCP, PF4J, Vaadin are adapters.
- Every tool source must normalize into one Tool Registry + Governed Tool Invocation Pipeline.
- Do not start with MCP or dynamic plugins; they depend on stable tool/policy/event contracts.
- Web GUI is an Agent Web Console first: Agent Catalog, Chat entry, Run timeline, Tool cards, Approval cards, Session history, plus Admin Governance. It is not a full visual workflow builder in v1.

## Open Questions

- Does v1 require crash-resumable durable execution, or persisted history/cancellation only?
- Which MCP transport/auth scope is mandatory for launch?
- Should dynamic plugins be restart-required for unload in v1?
- Which policy engine approach should be adopted beyond the default Java interface?
- Which E2E tool stack should be standard for Web Console: Playwright, Selenium, or Vaadin TestBench?

## Execution Progress

- [x] 01-01-PLAN.md — Java 21 Maven/COLA skeleton and architecture gates completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-01-SUMMARY.md`).
- [x] 01-02-PLAN.md — Define AgentDefinition, runtime state, error, and RunEvent contracts completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-02-SUMMARY.md`).
- [x] 01-03-PLAN.md — Define Workspace, Artifact/Attachment, and append-only Session tree contracts completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-03-SUMMARY.md`).
- [x] 01-04-PLAN.md — Implement runtime ports and reusable fake General Agent testkit loop completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-04-SUMMARY.md`).
- [x] 01-05-PLAN.md — Harden Phase 1 verification and write downstream contract index completed (`.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-05-SUMMARY.md`).

## Decisions

- 2026-06-13 (Phase 01 Plan 01): Use a Maven parent with explicit dependency/plugin management and no framework BOM in Phase 1 foundation.
- 2026-06-13 (Phase 01 Plan 01): Keep `pi-agent-domain` production dependencies empty while allowing only test-scoped JUnit, AssertJ, and ArchUnit.
- 2026-06-13 (Phase 01 Plan 01): Codify COLA dependency direction immediately with ArchUnit before domain implementation starts.
- [Phase 01]: Model Workspace as a logical runtime boundary using resource and mount IDs instead of host filesystem paths.
- [Phase 01]: Keep command execution in Domain as a request/result port only; no shell or process implementation belongs in Phase 1 Domain.
- [Phase 01]: Represent session history as an append-only tree with current leaf reconstruction and separated non-message context lists.
- [Phase 01]: Use nested JDK records in PlatformIds for runtime context IDs.
- [Phase 01]: Represent generic runtime input and event payload variants with Java sealed interfaces.
- [Phase 01]: Keep Domain contracts framework-free and serialization-neutral for adapter/client mapping later.
- [Phase 01]: Keep runtime/model/tool/event/policy ports pure Domain contracts with no async framework, provider SDK, Spring, persistence, or host process dependency.
- [Phase 01]: Implement GeneralAgentLoop in pi-testkit as a synchronous fake runtime for contract verification without real model keys or tools.
- [Phase 01]: Use exactly one terminal RunEvent as the last observable fake loop outcome.
- [Phase 01]: Use Java 21 explicitly for Maven verification in this environment because the system Maven launcher defaults to Java 17.
- [Phase 01]: Keep Phase 1 contract documentation as a downstream boundary document that names deferred provider, persistence, UI, MCP, plugin, shell, filesystem, and durable execution scopes.
- [Phase 02]: Plan 01 imported Spring Boot and Testcontainers through root dependencyManagement while keeping module dependencies explicit.
- [Phase 02]: Plan 01 kept Spring/JDBC/Flyway/Security dependencies only in outer Client/Infrastructure/Adapter modules; Domain and App production dependencies remain framework-isolated.
- [Phase 02]: Plan 03 keeps event history REST and SSE on one shared RunEventDto envelope in pi-agent-client.
- [Phase 02]: Plan 03 keeps client event DTOs provider-neutral and free of Domain imports so adapter-web owns Domain-to-public mapping later.
- [Phase 02]: Plan 03 exposes event replay through run-scoped sequence cursors afterSequence and nextAfterSequence.
- [Phase 02]: Plan 02 kept pi-agent-client DTOs as plain Java records without Domain imports or Spring/Jakarta annotations.
- [Phase 02]: Plan 02 represented create-run input generically with inputType plus Map input instead of chat-transcript-only API shape.
- [Phase 02]: Plan 04 uses plain App-layer records for security and correlation context without Spring or servlet dependencies.
- [Phase 02]: Plan 04 keeps run use-case methods session-centric by requiring sessionId alongside runId for commands and queries.
- [Phase 02]: Plan 04 configures Surefire to allow filtered reactor builds where upstream modules have no matching tests.
- [Phase 02]: Plan 05 keeps persistence and execution contracts in pi-agent-app as plain Java ports depending only on App, Client, and Domain types.
- [Phase 02]: Plan 05 models queued cancellation as Optional<QueuedRun> cancelQueuedAndReturn(...) so App services can publish exactly one durable terminal event with original queued context.
- [Phase 02]: Plan 05 makes terminal publishing methods publish*IfAbsent to reserve durable hasTerminalEvent idempotency guards.
- [Phase 02]: Plan 06 kept concrete App use-case services as plain Java constructor-injected classes with no Spring/JDBC/Servlet/SSE imports.
- [Phase 02]: Plan 06 routes App-created terminal run events through DefaultRunTerminalEventPublisher guarded by RunEventStore.hasTerminalEvent.
- [Phase 02]: Plan 06 uses queued-run context for queued cancellation terminal events to preserve tenant/user/session/run/workspace/trace/correlation IDs.

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|-------|------|----------|-------|-------|
| 01-runtime-spine-workspace-and-domain-contracts | 01 | 11m 46s | 2 | 16 |
| Phase 01-runtime-spine-workspace-and-domain-contracts P03 | 6m 34s | 3 tasks | 19 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P02 | 7m 03s | 3 tasks | 22 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P04 | 7m 20s | 3 tasks | 22 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P05 | 4m 06s | 3 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P01 | 3m 06s | 1 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P03 | 2m 24s | 1 tasks | 4 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P02 | 2m 42s | 1 tasks | 13 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P04 | 3m 30s | 1 tasks | 9 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P05 | 3m 01s | 1 tasks | 11 files |
| Phase 02-cloud-server-persistence-sse-and-baseline-security P06 | 7m 14s | 2 tasks | 9 files |

## Last Session

- **Updated:** 2026-06-13T18:55:00Z
- **Stopped At:** Completed 02-06-PLAN.md

## Next Action

Run:

```text
/gsd-verify-work 1
```

Context captured:

```text
.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md
```

Optional shortcuts:

```text
/gsd-plan-phase 1 --skip-research
```

## Session Notes

- 2026-06-13: Phase 1 context gathered. Resume from `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md`.

---
*State initialized: 2026-06-13 after roadmap creation*
