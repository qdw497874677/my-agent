# Project State: Pi Java Agent Platform

**Initialized:** 2026-06-13  
**Status:** Ready for Phase 1 planning  
**Current Phase:** Phase 1 — Runtime Spine and Domain Contracts

## Project Reference

See: `.planning/PROJECT.md` (updated 2026-06-13)

**Core value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。  
**Current focus:** Phase 1 — Runtime Spine and Domain Contracts

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
| 1 | Runtime Spine and Domain Contracts | Pending |
| 2 | Cloud Server, Persistence, SSE, and Baseline Security | Pending |
| 3 | Model Provider Registry and OpenAI-Compatible Adapter | Pending |
| 4 | Governed Tool Registry and Invocation Pipeline | Pending |
| 5 | Agent Web Console and Runtime Cockpit | Pending |
| 6 | Java Extension Surface: SPI and Spring | Pending |
| 7 | MCP Client Bridge and Governed Remote Tools | Pending |
| 8 | Controlled Dynamic Plugin JARs | Pending |
| 9 | Observability, Policy, Tenancy, and Production Hardening | Pending |

## Phase 1 Summary

**Goal:** Establish a framework-independent Java Agent Runtime kernel that all cloud, GUI, provider, tool, MCP, and plugin work will build on.

**Requirements:** CORE-01, CORE-02, CORE-03, CORE-04, CORE-05, CORE-06, CORE-07, CORE-08, OPS-04, OPS-06

**Key concerns for planning:**
- Keep runtime core independent of Spring Boot, Vaadin, PF4J, MCP, and provider SDKs.
- Keep runtime core generic: no Chat-only, Coding-only, single-provider, single-tool-protocol, or UI-driven assumptions.
- Define stable domain models before persistence/API/UI.
- Define event envelope carefully because REST/SSE/Admin/TUI/CLI/audit all depend on it.
- Use fake model/fake tool testkit to validate Agent loop without real providers.

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

## Next Action

Run:

```text
/gsd-discuss-phase 1
```

or skip discussion and run:

```text
/gsd-plan-phase 1
```

---
*State initialized: 2026-06-13 after roadmap creation*
