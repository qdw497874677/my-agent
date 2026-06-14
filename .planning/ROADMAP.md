# Roadmap: Pi Java Agent Platform

**Created:** 2026-06-13  
**Granularity:** Standard  
**Mode:** YOLO  
**v1 Requirements:** 75  
**Mapped:** 75 / 75 ✓

## Overview

Pi Java Agent Platform will be built as a dependency-driven Java cloud Agent platform: runtime contracts first, Cloud Server and persistence next, model/provider integration, governed tools, Agent Web Console + Admin Governance, Java extension surfaces, MCP, dynamic plugins, and finally production hardening. The roadmap intentionally avoids starting with MCP/PF4J/TUI because those capabilities depend on stable event, tool, policy, and persistence contracts.

| # | Phase | Goal | Requirements | UI hint |
|---|-------|------|--------------|---------|
| 1 | Runtime Spine, Workspace, and Domain Contracts | Establish COLA-aligned, Spring-free, UI-agnostic Agent Runtime and Workspace contracts, state model, interaction model, event envelope, cancellation, and testkit | CORE-01..CORE-09, WORK-01, WORK-02, WORK-04, WORK-05, OPS-04, OPS-06 | no |
| 2 | Cloud Server, Persistence, SSE, and Baseline Security | Expose runtime through Spring Boot REST/SSE with durable PostgreSQL state, baseline security, and first headless E2E | CLOUD-01..CLOUD-06, E2E-01, E2E-04, E2E-05 | no |
| 3 | Model Provider Registry and OpenAI-Compatible Adapter | Add real streaming model IO, provider registry, usage/error normalization, and credential boundaries | MODEL-01..MODEL-05 | no |
| 4 | Governed Tool Registry, Workspace, and Invocation Pipeline | Build the single safety gateway for all tool and workspace execution with schema, policy, timeout, audit, redaction, provision preview, and security E2E | WORK-03, WORK-07, WORK-08, TOOL-01..TOOL-07, OPS-02, OPS-03, OPS-05, E2E-02, E2E-03, E2E-06 | no |
| 5 | Agent Web Console and Runtime Cockpit | Provide all-Java Agent Catalog, Chat entry, run timeline, tool cards, approval cards, session history, admin governance views, and browser E2E | GUI-01..GUI-08, E2E-07 | yes |
| 6 | Java Extension Surface: SPI and Spring | Stabilize public extension APIs via Java SPI and Spring Bean/annotation registration | WORK-06, EXT-01..EXT-05 | no |
| 7 | MCP Client Bridge and Governed Remote Tools | Connect trusted MCP servers and normalize remote tools through the governed tool pipeline | MCP-01..MCP-05, E2E-08 | yes |
| 8 | Controlled Dynamic Plugin JARs | Load trusted plugin JARs with lifecycle, compatibility checks, health, disable, and quarantine | PLUG-01..PLUG-06, E2E-08 | yes |
| 9 | Observability, Policy, Tenancy, and Production Hardening | Complete production safety and operational readiness across traces, audit, tenant context, and metrics | OPS-01 | yes |

## Phase Details

### Phase 1: Runtime Spine, Workspace, and Domain Contracts

**Goal:** Establish a framework-independent and UI-agnostic Java Agent Runtime kernel plus first-class Workspace contracts in the COLA Domain layer that all cloud, GUI, provider, tool, MCP, and plugin work will build on.

**Requirements:** CORE-01, CORE-02, CORE-03, CORE-04, CORE-05, CORE-06, CORE-07, CORE-08, CORE-09, WORK-01, WORK-02, WORK-04, WORK-05, OPS-04, OPS-06  
**UI hint**: no

**Plans:** 5 plans

Plans:
- [x] 01-01-PLAN.md — Create Java 21 Maven/COLA skeleton and architecture gates.
- [x] 01-02-PLAN.md — Define AgentDefinition, runtime state, error, and RunEvent contracts.
- [x] 01-03-PLAN.md — Define Workspace, Artifact/Attachment, and append-only Session tree contracts.
- [x] 01-04-PLAN.md — Implement runtime ports and reusable fake General Agent testkit loop.
- [x] 01-05-PLAN.md — Harden Phase 1 verification and write downstream contract index.

**Success criteria:**
1. Developer can construct an `AgentDefinition` with model config, instructions, tool allowlist, policies, interaction modes, and runtime limits without Spring dependencies.
2. Runtime domain model includes `Session`, `Run`, `Step`, `Message`, `ToolCall`, `ToolResult`, `Artifact`, `Attachment`, `Workspace`, `WorkspaceSession`, `WorkspaceScope`, `WorkspaceSnapshot`, and `RunEvent` with tenant/user/session/run/step/trace/workspace context.
3. Runtime supports chat-style input, task/run input, structured form input, tool-driven execution, and future workflow/planner execution without using chat transcript as the only state model.
4. Domain defines `WorkspaceGateway`, `CommandExecutionGateway`, `Resource/Mount` abstractions, and snapshot contracts without host filesystem assumptions.
5. Fake model, fake tool, and fake workspace testkit can execute a complete General Agent loop and emit ordered events.
6. Runtime supports cancellation, max-step/deadline budget hooks, and terminal run states.
7. Architecture tests verify COLA boundaries: Adapter depends on App, App depends on Domain/Gateways, Infrastructure implements Domain ports, and Domain/core modules do not depend on Spring Boot, Vaadin, PF4J, MCP, DB, or provider SDKs.

**Notes:** This phase is the foundation. Avoid adding real provider SDKs, persistence, UI, MCP, or plugin classloaders to Domain. Those belong to Infrastructure/Adapter phases.

---

### Phase 2: Cloud Server, Persistence, SSE, and Baseline Security

**Goal:** Prove Pi is a Cloud Server product by exposing runtime through REST/SSE and durable PostgreSQL-backed state.

**Requirements:** CLOUD-01, CLOUD-02, CLOUD-03, CLOUD-04, CLOUD-05, CLOUD-06, E2E-01, E2E-04, E2E-05  
**UI hint**: no

**Plans:** 13 plans

Plans:
- [x] 02-01-PLAN.md — Add Phase 2 Maven dependency and verification foundation.
- [x] 02-02-PLAN.md — Define public session/run REST DTO contracts.
- [x] 02-03-PLAN.md — Define public RunEvent DTO and event-history contract.
- [x] 02-04-PLAN.md — Define App request context and use-case interfaces.
- [x] 02-05-PLAN.md — Define App persistence, audit, queue, dispatcher, and cancellation ports.
- [x] 02-06-PLAN.md — Implement concrete App use-case services and idempotent cancellation orchestration.
- [x] 02-07-PLAN.md — Implement PostgreSQL/Flyway/JDBC persistence and persist-then-emit event sink.
- [x] 02-08-PLAN.md — Implement DB queue, cancellation registry, and run dispatcher/worker.
- [x] 02-09-PLAN.md — Build Spring Boot shell, security baseline, and correlation filter.
- [x] 02-10-PLAN.md — Implement session-centric REST controllers and event mapper.
- [x] 02-11-PLAN.md — Implement SSE replay-before-subscribe streaming and subscription cleanup.
- [x] 02-12-PLAN.md — Wire the single Spring runtime composition root and worker activation path.
- [x] 02-13-PLAN.md — Add headless E2E, API docs, and requirement status updates.

**Success criteria:**
1. Authenticated REST API can create a run, fetch run detail, fetch status, list events, list steps/messages/tool calls, and cancel a run.
2. SSE stream emits the same provider-neutral RunEvent envelope stored in persistence.
3. PostgreSQL Flyway migrations create durable tables for sessions, runs, steps, messages, tool calls, events, and audit basics.
4. Cancellation through REST changes run state and appears in event history.
5. Health endpoints, structured logs, request correlation IDs, and tenant/user placeholder context are present.
6. Headless E2E can create a run, stream events, persist state, query history, cancel a run, and verify SSE ordering/reconnect behavior without real model keys.

**Notes:** Use API-first patterns so Web Console, Admin Governance, and future TUI/CLI consume the same surface.

---

### Phase 3: Model Provider Registry and OpenAI-Compatible Adapter

**Goal:** Add real model streaming and tool-call intent normalization without leaking provider-specific types into core.

**Requirements:** MODEL-01, MODEL-02, MODEL-03, MODEL-04, MODEL-05  
**UI hint**: no

**Plans:** 8 plans

Plans:
- [x] 03-01-PLAN.md — Define provider/model registry Domain contracts and secret-reference value objects.
- [x] 03-02-PLAN.md — Define provider-neutral streaming model chunks, metadata, errors, and event payloads.
- [x] 03-03-PLAN.md — Add App provider registry and SecretResolver ports plus provider query use case.
- [x] 03-04-PLAN.md — Evolve the fake General Agent loop and testkit for streaming model contracts.
- [x] 03-05-PLAN.md — Add isolated OpenAI-compatible provider infrastructure module, config, registry, and secret resolver.
- [x] 03-06-PLAN.md — Implement Spring AI-backed OpenAI-compatible StreamingModelClient with normalization and resilience.
- [x] 03-07-PLAN.md — Wire provider registry/adapter into Cloud Server composition and explicit provider:model dispatch.
- [x] 03-08-PLAN.md — Add fake provider contract tests, Cloud Server E2E, optional smoke test, and downstream docs.

**Success criteria:**
1. Provider registry resolves model IDs, provider IDs, capability descriptors, and credential references.
2. OpenAI-compatible streaming chat adapter can run through the General Agent loop.
3. Text deltas, tool-call intents, finish reasons, usage/tokens, latency, and provider errors are normalized into platform records/events.
4. Raw secrets are never written to logs, prompts, Admin-visible payloads, or default events.
5. Provider calls support timeout, cancellation, retry/rate-limit/circuit-breaker hooks, and contract tests.

**Research needed:** Spring AI artifact/version details; OpenAI-compatible streaming and tool-call semantics.

---

### Phase 4: Governed Tool Registry, Workspace, and Invocation Pipeline

**Goal:** Build the single safety gateway for every future tool source and workspace action before exposing SPI, Spring, MCP, or dynamic plugin tools.

**Requirements:** WORK-03, WORK-07, WORK-08, TOOL-01, TOOL-02, TOOL-03, TOOL-04, TOOL-05, TOOL-06, TOOL-07, OPS-02, OPS-03, OPS-05, E2E-02, E2E-03, E2E-06  
**UI hint**: no

**Plans:** 8 plans

Plans:
- [x] 04-01-PLAN.md — Define framework-free tool descriptor, execution result, preview, and lifecycle event contracts.
- [x] 04-02-PLAN.md — Add descriptor-first ToolRegistry, executor binding, query use case, and catalog DTO contracts.
- [x] 04-03-PLAN.md — Implement App ToolExecutionGateway orchestration for validation, policy, preview, audit, events, limits, and redaction.
- [x] 04-04-PLAN.md — Add Infrastructure JSON Schema validation, in-memory registry, default policy, redaction, payload limits, and preview generation.
- [x] 04-05-PLAN.md — Reroute testkit GeneralAgentLoop through ToolExecutionGateway and add gateway-aware fakes.
- [x] 04-06-PLAN.md — Implement bounded local-temp workspace, allowlisted command gateway, and safe built-in example tools.
- [x] 04-07-PLAN.md — Wire governed tools into Cloud Server, event DTO mapping, and read-only Tool Registry REST API.
- [ ] 04-08-PLAN.md — Add governed tool Cloud Server E2E, security redaction E2E, docs, and requirement status updates.

**Success criteria:**
1. Tools register with canonical descriptors including schema, provenance, version, scopes, risk level, side effects, and timeout defaults.
2. Workspace-backed file/command/resource actions execute through `WorkspaceGateway` / `CommandExecutionGateway` and remain subject to `ToolExecutionGateway` governance where exposed as tools.
3. All tool calls execute through `ToolExecutionGateway`; no provider or extension path can bypass it.
4. Gateway validates arguments, enforces timeout/cancellation/payload limits, supports provision/preview before risky execution, normalizes results/errors, and emits lifecycle events.
5. Default policy engine can allow, deny, require approval, require sandbox, or block tool/workspace actions.
6. Audit records include redacted input/output summaries and security-sensitive actions never expose raw secrets by default.
7. E2E proves successful tool execution, policy deny, approval-required, provision preview, workspace-bound command/file execution, and secret-redaction paths through API/runtime, events, audit, and persistence.

**Research needed:** JSON Schema validation/versioning and policy decision schema.

---

### Phase 5: Agent Web Console and Runtime Cockpit

**Goal:** Deliver an all-Java Agent Web Console where users can discover Agents, chat, watch execution, handle approvals, continue sessions, and access basic Admin Governance for runtime health and controls.

**Requirements:** GUI-01, GUI-02, GUI-03, GUI-04, GUI-05, GUI-06, GUI-07, GUI-08, E2E-07  
**UI hint**: yes

**Success criteria:**
1. User can browse an Agent Catalog and enter a Chat page for a selected Agent.
2. User can send a message, receive streaming output, and see Run status/timeline updates through SSE.
3. User can see tool calls as cards with status, risk/side-effect label, progress, redacted result summary, and errors.
4. User can view/continue Session history and cancel a running Run.
5. User or Admin can approve/reject gated tool calls through approval cards when ToolPolicy requires approval.
6. Admin can inspect provider, extension, MCP, plugin, tool registry, policy decision, and audit governance views.
7. Web GUI uses REST/SSE/read-model APIs rather than private runtime/database access.
8. Browser E2E validates Agent Catalog, interaction page, streaming output, tool cards, approval cards, session history, cancellation, and basic governance views.

**Research needed:** Vaadin + Spring Security + SSE Chat UI patterns, tool-call card UX, approval-card flow, and Agent Catalog information architecture.

---

### Phase 6: Java Extension Surface: SPI and Spring

**Goal:** Stabilize the Java-native extension contract using safe in-process mechanisms before dynamic classloader plugins.

**Requirements:** EXT-01, EXT-02, EXT-03, EXT-04, EXT-05  
**UI hint**: no

**Success criteria:**
1. Public extension API/JAR supports tools, providers, policies, event sinks, memory providers, workspace providers, metadata, lifecycle, and version compatibility.
2. Java ServiceLoader discovers extension capabilities and registers them through the platform registry.
3. Spring Boot starter/autoconfiguration registers Spring Beans or annotations as tools/providers/policies/listeners without core changes.
4. Admin can view extension sources, capabilities, health, compatibility, enable/disable status, and errors.
5. Conformance tests prove extensions cannot bypass tool gateway, policy, audit, event, or credential boundaries.

**Notes:** This phase establishes the public SDK shape before PF4J.

---

### Phase 7: MCP Client Bridge and Governed Remote Tools

**Goal:** Add MCP as a remote tool adapter that respects Pi's registry, policy, audit, and event model.

**Requirements:** MCP-01, MCP-02, MCP-03, MCP-04, MCP-05, E2E-08  
**UI hint**: yes

**Success criteria:**
1. Admin can configure trusted MCP servers with credential references, transport settings, server allowlists, and network controls.
2. Platform discovers MCP tools, normalizes their schemas into `ToolDescriptor`, and registers them with provenance/server health.
3. MCP tool calls execute only through `ToolExecutionGateway` with policy, timeout, cancellation, audit, redaction, and events.
4. Admin can see MCP connection state, discovery errors, invocation errors, auth failures, and server health.
5. Security-sensitive boundaries for SSRF, credentials, and transport configuration are implemented or explicitly blocked by defaults.
6. Integration E2E proves Fake MCP server discovery and tool execution through ToolExecutionGateway, policy, audit, and event pipeline.

**Research needed:** MCP Java SDK/Spring AI MCP maturity, transports, OAuth/protected-resource auth, SSRF controls.

---

### Phase 8: Controlled Dynamic Plugin JARs

**Goal:** Support trusted dynamic plugin JARs as controlled enterprise extensions with lifecycle, compatibility, and operational controls.

**Requirements:** PLUG-01, PLUG-02, PLUG-03, PLUG-04, PLUG-05, PLUG-06, E2E-08  
**UI hint**: yes

**Success criteria:**
1. Admin can configure a controlled plugin directory for trusted plugin JARs.
2. Platform loads descriptors, validates API/platform compatibility, and registers capabilities through the extension registry.
3. Plugin lifecycle states include discovered, loaded, started, disabled, failed, and quarantined.
4. Admin can view plugin metadata, capabilities, health, load errors, compatibility errors, and disable/quarantine plugins.
5. Documentation and runtime warnings make clear that JVM plugin isolation is not a sandbox for untrusted code.
6. Integration E2E proves sample plugin JAR load, capability registration, ToolGateway invocation, disable, and quarantine behavior.

**Research needed:** PF4J vs alternatives, Spring Boot executable JAR packaging, classloader behavior, unload semantics.

---

### Phase 9: Observability, Policy, Tenancy, and Production Hardening

**Goal:** Complete production-readiness around telemetry, security, audit, tenancy context, and operational reliability.

**Requirements:** OPS-01  
**UI hint**: yes

**Success criteria:**
1. Platform emits structured logs, metrics, and OpenTelemetry-compatible spans for run, model, tool, MCP, plugin, and policy lifecycles.
2. Web Console/Admin Governance surfaces runtime health and key operational metrics for runs, providers, tools, MCP servers, and plugins.
3. Trace/run/session IDs are consistently correlated across API responses, SSE events, logs, audit records, and traces.
4. Production configuration documents cover secrets, policy engine extension, tenancy/RBAC hooks, sandbox strategy, retention/redaction, and deployment.
5. Regression tests cover critical policy, audit, cancellation, timeout, extension, and event-ordering paths.

**Research needed:** policy engine options, secrets/KMS, tenancy/RBAC, sandbox strategy, observability backend.

## Coverage Validation

| Requirement Prefix | Count | Phase |
|--------------------|-------|-------|
| CORE | 9 | Phase 1 |
| WORK | 8 | Phase 1, 4, 6 |
| CLOUD | 6 | Phase 2 |
| MODEL | 5 | Phase 3 |
| TOOL | 7 | Phase 4 |
| EXT | 5 | Phase 6 |
| MCP | 5 | Phase 7 |
| PLUG | 6 | Phase 8 |
| GUI | 8 | Phase 5 |
| OPS | 6 | Phase 1, 4, 9 |
| E2E | 8 | Phase 2, 4, 5, 7, 8 |

**Total mapped:** 75 / 75 ✓

## Deferred After v1

- TUI/CLI clients over the same REST/SSE API.
- Local developer runtime mode.
- Multi-agent orchestration and workflow builder.
- Durable crash-resumable execution and replay.
- RAG/knowledge-base product.
- Sandboxed Coding Agent workspace.
- Plugin marketplace and full SaaS tenant/RBAC/billing.

---
*Roadmap created: 2026-06-13 after initialization*
