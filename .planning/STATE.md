---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
current_phase: 01
status: executing
stopped_at: Completed 01-04-PLAN.md
last_updated: "2026-06-13T19:05:25.490Z"
progress:
  total_phases: 9
  completed_phases: 0
  total_plans: 5
  completed_plans: 4
---

# Project State: Pi Java Agent Platform

**Initialized:** 2026-06-13  
**Status:** Executing Phase 01 — 3/5 plans complete
**Current Phase:** 01

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-13)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 01 — runtime-spine-workspace-and-domain-contracts

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
| 1 | Runtime Spine, Workspace, and Domain Contracts | Pending |
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
- [ ] 01-04-PLAN.md — Implement runtime ports and reusable fake General Agent testkit loop.
- [ ] 01-05-PLAN.md — Harden Phase 1 verification and write downstream contract index.

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

## Performance Metrics

| Phase | Plan | Duration | Tasks | Files |
|-------|------|----------|-------|-------|
| 01-runtime-spine-workspace-and-domain-contracts | 01 | 11m 46s | 2 | 16 |
| Phase 01-runtime-spine-workspace-and-domain-contracts P03 | 6m 34s | 3 tasks | 19 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P02 | 7m 03s | 3 tasks | 22 files |
| Phase 01-runtime-spine-workspace-and-domain-contracts P04 | 7m 20s | 3 tasks | 22 files |

## Last Session

- **Updated:** 2026-06-13T18:55:00Z
- **Stopped At:** Completed 01-04-PLAN.md

## Next Action

Run:

```text
/gsd-execute-phase 1 --plan 01-04
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
