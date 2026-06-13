# Project Research Summary

**Project:** Pi Java Agent Platform  
**Domain:** Java cloud Agent runtime/platform with Admin GUI, extensibility, MCP, policy, audit, and observability  
**Researched:** 2026-06-13  
**Confidence:** MEDIUM-HIGH

## Executive Summary

Pi Java should be built as a production-grade **Java cloud Agent runtime/platform**, not as a thin LLM wrapper, visual workflow builder, or local CLI/TUI port. Expert agent platforms converge on the same baseline: durable agent/session/run state, structured streaming events, governed tool execution, model-provider abstraction, extension mechanisms, auditability, observability, and operational controls. The product’s strongest v1 positioning is a **Java-native, Spring-friendly runtime kernel** that enterprises can embed, extend, inspect, and govern.

The recommended approach is a **modular monolith first, distributed-capable later**: a Java 21 + Spring Boot 3.5.x Cloud Server around a framework-independent runtime core. The core should own the domain contracts and state machine (`AgentRuntime`, `Run`, `Step`, `Tool`, `Provider`, `Session`, `Policy`, `EventSink`, `Plugin`), while Spring Boot, Spring AI, MCP, PF4J, PostgreSQL, Vaadin, and observability libraries remain adapters. Start with a reliable General Agent loop, REST/SSE API, persistence, event model, OpenAI-compatible provider, and a single governed Tool Invocation Pipeline before adding SPI/Spring extensions, MCP, and dynamic plugins.

The main risk is the combination of **cloud execution + extensibility + tool authority**. SPI, Spring Beans, dynamic plugin JARs, and remote MCP are independently reasonable, but together they can create lifecycle ambiguity, policy bypasses, classloader leaks, confused-deputy auth flaws, and unsafe tool execution. Mitigate this by forcing every tool source through one normalized registry and deterministic gateway with schema validation, policy, approval/sandbox hooks, deadlines, cancellation, audit, telemetry, and tenant/user/session context.

## Key Findings

### Recommended Stack

Build on a conservative, enterprise-friendly Java stack: **Java 21 LTS**, **Maven**, **Spring Boot 3.5.x**, **Spring AI 1.1.x stable**, **Spring MVC + virtual threads**, **PostgreSQL**, **JDBC/Spring Data JDBC**, **Flyway**, **Micrometer/OpenTelemetry**, **Resilience4j**, **Spring Security**, **PF4J for controlled dynamic plugins**, and **Vaadin Flow** for the v1 Admin GUI. Prefer stable BOM-managed versions and validate patch versions at implementation time, especially Spring AI/MCP/PF4J/Vaadin artifacts.

The key stack principle is adapter containment: **do not let Spring AI, MCP, PF4J, Vaadin, LangChain4j, or any provider SDK define the product’s core model**. Pi’s durable value is its runtime contract and governed extension fabric. Use Spring AI for provider/tool/MCP integration where it helps, but keep Pi’s provider/tool/event/policy abstractions narrower and framework-independent.

**Core technologies:**
- **Java 21 LTS** — runtime and SDK baseline; gives virtual threads and broad enterprise support without Java 25 adoption friction.
- **Maven 3.9.x+** — multi-module enterprise Java build and BOM publishing workflow.
- **Spring Boot 3.5.x** — Cloud Server, REST/SSE APIs, configuration, Actuator, and security baseline.
- **Spring AI 1.1.x stable** — default model/provider/tool/MCP integration adapter; avoid Spring AI 2.0 milestones as v1 baseline.
- **Spring MVC + virtual threads** — simpler fit for blocking model/tool calls than full reactive architecture; use WebFlux only at inherently reactive boundaries.
- **Official MCP Java SDK / Spring AI MCP** — remote tool protocol adapter; MCP must normalize into Pi’s internal registry/policy model.
- **Java SPI + Spring Bean discovery + PF4J** — staged extension model: start with SPI/Spring, add controlled dynamic plugin JARs after contracts stabilize.
- **PostgreSQL 17/18 + JSONB** — durable system of record for sessions, runs, events, tool calls, audit, configuration, and flexible payloads.
- **JDBC/Spring Data JDBC + Flyway** — explicit SQL and migrations for append-heavy event/audit data; avoid JPA for core run/event persistence.
- **Micrometer + OpenTelemetry + Actuator** — first-class runtime telemetry for run/model/tool/plugin/MCP lifecycle.
- **Resilience4j** — retry, rate limit, circuit breaker, bulkhead, and time limiter around models, MCP, and remote tools.
- **Spring Security** — OAuth2 Resource Server/JWT-ready boundary for API/Admin security; avoid custom auth.
- **Vaadin Flow 24.x** — all-Java operational Admin GUI; avoid React/Next.js for v1 unless UI becomes product-critical.
- **Testcontainers, fake providers/tools, ArchUnit** — integration and boundary tests are essential for state, audit, and module isolation.

### Table Stakes

Pi v1 must be credible as a **cloud General Agent runtime**. Missing any of these will make the platform feel incomplete, unsafe, or hard to operate.

**Must have (table stakes):**
- **Agent definition/configuration** — model, instructions, tool allowlist, policies, and runtime options.
- **Run/session lifecycle API** — create, stream, cancel, inspect, and query history.
- **Structured streaming event protocol** — provider-neutral SSE events for model deltas, steps, tools, policy, errors, and terminal states.
- **Explicit state model** — `Session`, `Run`, `Step`, `Message`, `ToolCall`, `ToolResult`, `RunEvent`, not just chat transcripts.
- **OpenAI-compatible model provider abstraction** — first real provider while preserving provider-swappable capability model.
- **Function/tool-calling loop** — one reliable General Agent loop before graph/multi-agent features.
- **Unified tool registry** — one metadata model for built-in Java tools, SPI tools, Spring Bean tools, plugin tools, and MCP tools.
- **Tool execution engine/gateway** — schema validation, timeout, cancellation, error handling, result normalization.
- **Tool policy and approval hooks** — deterministic allow/deny/approval decisions before tool execution.
- **Tool audit trail** — who/what/why/when/with-which-authority records for every tool call.
- **Persistence abstraction and PostgreSQL-backed default** — durable run history and event/audit records from day one.
- **REST/SSE API-first backend** — Admin GUI and future CLI/TUI consume the same protocol.
- **Admin GUI runtime cockpit** — run timeline, event stream, tool-call audit, provider/plugin/MCP health, operational controls.
- **Java SPI and Spring Bean extension paths** — lowest-risk Java-native extension surfaces.
- **Remote MCP client integration** — discover and invoke MCP tools through the same policy/audit path.
- **Controlled dynamic plugin loading** — plugin directory, metadata, compatibility, lifecycle, health, disable/quarantine; true hot unload can wait.
- **Secrets/config boundary** — `SecretRef` abstraction; no raw secrets in prompts, logs, or Admin GUI.
- **Authentication, cancellation, timeouts, retries, observability, token/cost accounting, and workspace/sandbox abstraction** — required production safety surfaces.

### Differentiators

Pi should compete by being the **Java-native governed agent runtime**, not by copying Python/TypeScript agent frameworks or Dify-style builders.

**Should have (competitive):**
- **Framework-independent Java Agent Runtime Kernel** — strongest differentiator for enterprise JVM teams.
- **Unified extension fabric** — SPI, Spring Beans, dynamic plugin JARs, and MCP normalized into one registry/policy/audit model.
- **Governed tool fabric** — policy and audit across local, plugin, and remote MCP tools.
- **Admin GUI as runtime cockpit** — operational debugging and controls instead of no-code workflow editing.
- **Dynamic plugin lifecycle for trusted enterprise extensions** — install/load/disable/quarantine custom tools/providers/policies without core rebuilds.
- **MCP client/gateway governance** — remote MCP tools brokered through Java policy, audit, tenancy, and schema normalization.
- **Side-effect classification for tools** — read/write/external-impact/file/network/shell risk metadata visible to policy and UI.
- **Pluggable policy engine** — custom approval/RBAC/ABAC/quota/compliance checks via Java interfaces first.
- **Durable replay/debug-ready event log** — design now; full replay/evals can come later.
- **Extension SDK with compatibility tests** — better v1 ecosystem investment than a marketplace.
- **Provider/tool simulation mode** — fake providers and tool stubs for deterministic Java CI.

### Anti-Features

Explicitly avoid v1 scope creep that changes the product category or compromises safety.

**Defer / avoid for v1:**
- **Full Dify-style visual workflow builder** — different product surface and high UI complexity; build runtime cockpit instead.
- **Full plugin marketplace** — distribution, trust, review, billing, and moderation are out of scope; support private/local plugins first.
- **Unrestricted shell/file/code execution** — too risky for cloud v1; require workspace/sandbox/policy and keep disabled by default.
- **Every model provider** — ship OpenAI-compatible first; add Anthropic/Gemini/Bedrock later through capability-based adapters.
- **Recreating the TypeScript CLI/TUI** — future clients should use REST/SSE, not shape the core.
- **Multi-agent orchestration as centerpiece** — single-agent runtime, state, policy, and observability must stabilize first.
- **Autonomous plugin installation by agents** — severe supply-chain risk; plugin lifecycle is admin-only and audited.
- **Global mutable singleton registries** — incompatible with tenancy, tests, plugin reload, and scoped policies.
- **Vendor-specific provider leakage in core** — use normalized messages/tool calls/capabilities.
- **Storing full sensitive tool payloads by default** — use redaction, summaries, and configurable secure retention.
- **Guaranteed hot unload in v1** — use load/disable/restart-required unload semantics first.
- **Heavy RAG/knowledge-base product** — define Memory/Retrieval extension points; defer full ingestion/vector features.
- **Consumer chat app primary UI** — provide API and simple run tester if needed; Admin GUI is operational.

### Architecture Approach

Use a **ports-and-adapters modular monolith** with explicit module boundaries so v1 can ship as one Spring Boot Cloud Server while remaining ready to split control plane, execution plane, event fanout, plugin execution, or MCP gateway later. The runtime core must be Spring-free and own state transitions, event emission, model/tool ports, and cancellation. Application services coordinate use cases. Infrastructure modules implement persistence, provider adapters, MCP, plugins, observability, and security. Admin GUI consumes the same REST/SSE/read-model APIs as external clients.

**Major components:**
1. **Agent Runtime Kernel** — framework-agnostic single-agent loop, run state machine, event emission, cancellation checks.
2. **REST/SSE API Layer** — authenticated create/run/status/cancel/history endpoints and stable event stream.
3. **Application Services** — transactional orchestration for runs, sessions, tools, providers, plugins, and policies.
4. **Model Provider Registry** — resolve provider/model config and normalize requests, streams, tool-call intents, usage, and errors.
5. **Governed Tool Invocation Pipeline** — single path for schema validation, policy, approval/sandbox, timeout, execution, audit, persistence, and events.
6. **Tool Registry** — normalized `ToolDescriptor + ToolExecutorBinding` entries with provenance, version, scopes, risk, and health.
7. **Policy / Approval / Sandbox Ports** — deterministic extension point for authorization, gated actions, resource constraints, and future human approval.
8. **Extension / Plugin Manager** — discovers SPI, Spring Bean, dynamic JAR/PF4J, and MCP capabilities and registers them through application services.
9. **MCP Bridge** — MCP host/client manager that discovers remote tools/resources/prompts and maps tool calls into the internal pipeline.
10. **Persistence / Event Store** — PostgreSQL-backed sessions, runs, steps, messages, tool calls, events, audit, plugin metadata, and read models.
11. **Observability + Audit** — structured logs, metrics, OpenTelemetry spans, audit sinks, and shared IDs across events/traces.
12. **Admin GUI** — API-driven runtime cockpit for debugging, health, cancellation, approvals, and disabling risky capabilities.

**Key patterns to follow:**
- Runtime kernel behind ports; core depends only on API/contracts and small utilities.
- Append-only run events plus query-friendly read models.
- Unified tool descriptor and executor binding for every tool source.
- MCP as adapter, not the internal tool model.
- Dynamic plugin lifecycle behind Pi’s own plugin runtime port.
- Observability as first-class runtime output, not post-hoc logs.
- Namespace-qualified tool names (`pluginId.toolName`, `mcpServer.toolName`, `builtin.toolName`).
- Tenant/user/session/workspace IDs modeled from the start even if v1 runs single-tenant.

### Key Risks

1. **Extension mechanisms drive core abstractions** — define Spring-free descriptors, lifecycle states, executor bindings, and conformance tests before SPI/Spring/MCP/PF4J adapters.
2. **Tools bypass deterministic policy/audit** — all tool execution must go through one `ToolExecutionGateway`; model providers only surface tool-call intents.
3. **Dynamic plugins treated as safe sandboxing** — classloader isolation solves dependency/lifecycle issues, not malicious-code security; untrusted code requires out-of-process/container isolation.
4. **MCP auth is simplified into static tokens** — follow MCP authorization requirements, avoid token passthrough, validate resource/audience, handle `WWW-Authenticate`, use HTTPS/PKCE where applicable, and add SSRF controls.
5. **Agent inherits full user/admin authority** — effective authority must be intersection of tenant, user, agent, tool, session/workspace, and approval constraints.
6. **Prompt injection handled only by prompts** — track trust labels and enforce deterministic policy when untrusted tool/output content precedes sensitive actions.
7. **Run/session state stored as UI history** — persist structured execution events with stable IDs and separate event log, read models, and model context state.
8. **Streaming treated as token transport only** — define provider-neutral event envelopes, sequence IDs, terminal states, heartbeat/reconnect behavior.
9. **Provider abstraction hides capability differences** — use explicit provider capability descriptors, normalized event/error taxonomy, and provider contract tests.
10. **Admin GUI is read-only** — include operational controls: cancel, approve/reject, inspect policy, disable tool/plugin, quarantine bad capabilities.

## Implications for Roadmap

Based on all research, the roadmap should be dependency-driven: **contracts and event model first, cloud API/persistence second, real model/tool execution third, extension integrations after the governed pipeline exists, dynamic plugins and hardening last**. Do not start with MCP or PF4J; they are adapters over foundations.

### Phase 1: Runtime Spine and Domain Contracts

**Rationale:** Every later capability depends on stable runtime state, events, provider/tool ports, cancellation, and extension boundaries. This phase prevents Spring AI, MCP, PF4J, or the Admin GUI from owning the core abstractions.

**Delivers:**
- `pi-agent-api` / `pi-agent-core` modules.
- `AgentDefinition`, `Session`, `Run`, `Step`, `Message`, `ToolCall`, `ToolResult`, `RunEvent` model.
- Framework-independent `AgentRuntime`, `ModelClient`, `ToolInvoker`, `EventSink`, `Policy` ports.
- Ordered event envelope with IDs, sequence, timestamp, tenant/user/session/run/step, trace ID, payload, redaction metadata.
- Fake model/fake tool testkit.
- Cancellation/status state machine and max-step/deadline budget hooks.

**Addresses:** Agent definition, run/session lifecycle, event model, state model, provider abstraction, future API/GUI/CLI compatibility.

**Avoids:** UI-driven runtime, chat-history-only state, token-only streaming, provider SDK leakage, extension mechanisms driving core.

**Research flag:** Standard patterns; skip broad research. Validate exact event schema, state machine statuses, and durable checkpoint expectations.

### Phase 2: Cloud Server API, Persistence, SSE, and Baseline Security

**Rationale:** Pi is a Cloud Server first. Exposing the runtime through REST/SSE and PostgreSQL early prevents a local-only architecture and proves the API contract for Admin GUI and future CLI/TUI.

**Delivers:**
- Spring Boot 3.5.x server with REST endpoints: create run, get run, list events, cancel run, session history.
- SSE stream using the same persisted `RunEvent` envelope.
- PostgreSQL schema and Flyway migrations for sessions/runs/steps/messages/tool calls/events/audit basics.
- Minimal auth/security context and tenant placeholder fields.
- Actuator health/metrics baseline and structured logs with run/session/trace IDs.

**Addresses:** REST/SSE API-first backend, persistence abstraction, run history, cancellation, authentication boundary, observability hooks.

**Avoids:** Admin GUI private endpoints, missing tenant IDs, SSE reconnect gaps, logs-only debugging, raw secrets in config.

**Research flag:** Standard Spring/PostgreSQL patterns; research only if v1 requires crash-resumable durable execution rather than persisted history.

### Phase 3: Model Provider Registry and OpenAI-Compatible Adapter

**Rationale:** A real General Agent runtime needs model streaming and tool-call intent normalization before external tool complexity. OpenAI-compatible support gives broad provider coverage while preserving adapter boundaries.

**Delivers:**
- Provider registry and `ModelConfig` / `CredentialRef` model.
- OpenAI-compatible chat/streaming adapter through Spring AI where useful.
- Normalized model events, tool-call intents, usage/tokens, provider error taxonomy.
- Resilience4j retry/rate-limit/time-limit around provider calls.
- Provider contract tests and fake provider cases.

**Addresses:** Model provider abstraction, streaming, token/cost accounting foundation, capability-based provider design.

**Avoids:** OpenAI semantics becoming core, direct provider SDK types in public API, blind retries, wrong cost accounting.

**Research flag:** Needs targeted implementation research for exact Spring AI artifact names, OpenAI-compatible streaming/tool-call behavior, and Spring AI 1.1.x vs 2.x migration watch.

### Phase 4: Governed Tool Registry and Tool Invocation Pipeline

**Rationale:** Tool governance is the safety choke point and must exist before SPI/Spring extensions, MCP, or dynamic plugins expose more authority.

**Delivers:**
- Canonical `ToolDescriptor` with JSON Schema, provenance, version, scopes, risk/side-effect metadata, timeout defaults.
- `ToolExecutorBinding` and one `ToolExecutionGateway`.
- Validation, policy stub, approval/suspend hook, timeout/deadline, cancellation, result normalization.
- Audit records and runtime events for proposed/started/completed/failed/denied tool calls.
- Built-in safe example tools.
- Redaction and summaries for sensitive inputs/outputs.

**Addresses:** Tool registry, tool execution engine, policy/approval hooks, audit trail, workspace/sandbox abstraction hooks.

**Avoids:** Model/provider/plugin/MCP direct tool execution, prompt-based authorization, missing audit context, inconsistent failures, unsafe side effects.

**Research flag:** Standard patterns plus security-sensitive design. Research policy decision schema and JSON Schema validation/versioning during phase planning.

### Phase 5: Admin GUI Runtime Cockpit and Operational Controls

**Rationale:** The Admin GUI should validate operability and governance, not drive architecture. It becomes valuable once runs, events, models, and tool calls exist.

**Delivers:**
- Vaadin Flow operational UI consuming REST/SSE/read models.
- Run list/detail, event timeline, tool-call inspector, provider config/status.
- Cancel run, inspect policy decisions, view redacted payloads.
- Initial tool/provider health views and audit browsing.
- Token/latency/cost summaries where provider data is available.

**Addresses:** Admin run inspector, tool-call audit, operational visibility, basic cost/token accounting, API-first UI.

**Avoids:** Read-only dashboard without controls, direct DB/runtime access, future CLI/TUI protocol divergence.

**Research flag:** Needs targeted Vaadin/Spring Security/SSE UI research if the team has limited Vaadin experience; otherwise standard patterns.

### Phase 6: Java Extension Surface — SPI and Spring Bean Registration

**Rationale:** SPI and Spring Beans are the lowest-risk Java-native extension mechanisms and should stabilize the public extension API before classloader-based dynamic plugins.

**Delivers:**
- `pi-agent-api` / `pi-agent-spi` public extension JARs.
- Java `ServiceLoader` discovery for tools, providers, policies, event sinks, memory/workspace providers.
- Spring Boot starter/autoconfiguration and bean/annotation-based registration.
- Extension metadata, lifecycle states, health contributors, compatibility/version checks.
- Conformance tests for discovery, validation, enable/disable, policy deny, timeout, cancellation, audit, metrics.
- Sample extensions and test harness.

**Addresses:** Java SPI extension points, Spring-native registration, extension SDK, annotation-based tool registration.

**Avoids:** Spring dependency in core, lifecycle mismatches, plugin API versioning too late, ApplicationContext leakage, global registries.

**Research flag:** Standard Java/Spring patterns, but validate ServiceLoader behavior in Spring Boot executable JARs and binary compatibility policy.

### Phase 7: MCP Client Bridge and Governed Remote Tools

**Rationale:** MCP is high-value but must plug into the already governed registry and pipeline. Implementing it earlier risks bypassing policy/audit and mishandling auth.

**Delivers:**
- MCP connection manager and configured server lifecycle.
- Capability negotiation, `tools/list`, schema normalization, and health states.
- MCP tool descriptors and executor bindings through the ToolExecutionGateway.
- MCP events/metrics/audit including auth failures and server health.
- Initial transport scope decision (remote Streamable HTTP likely highest value; stdio may be useful for local/trusted setups).
- Manual trusted-server configuration first; protected-resource OAuth hardening before multi-tenant/protected use.

**Addresses:** Remote MCP client integration, MCP governance/audit, external tool fabric.

**Avoids:** Raw MCP as internal model, token passthrough, confused deputy auth, untrusted metadata poisoning, SSRF on metadata discovery.

**Research flag:** Needs deeper research. Validate current MCP Java SDK/Spring AI MCP maturity, auth spec implementation details, transport scope, and protected-resource/OAuth flows.

### Phase 8: Controlled Dynamic Plugin JARs

**Rationale:** Dynamic plugins are differentiating and risky. Build them only after extension contracts, registries, tool gateway, and conformance tests are stable.

**Delivers:**
- Plugin manifest format with API compatibility, permissions, metadata, provenance, and integrity hooks.
- PF4J or wrapped plugin manager integration behind Pi’s `PluginRuntime` port.
- One classloader per plugin, parent API loading, lifecycle states, health checks.
- Install/load/start/stop/disable/quarantine operations via audited Admin/API.
- Registry cleanup and active invocation safeguards on disable/uninstall.
- Packaging tests against Spring Boot executable JAR and conflict/unload tests.

**Addresses:** Controlled dynamic plugin loading, enterprise extensions, plugin listing/health, disable/quarantine.

**Avoids:** Classpath scanning, duplicate API classes, classloader leaks, false sandbox claims, direct plugin access to host internals.

**Research flag:** Needs deeper research. Compare PF4J/PF4J-Spring/custom classloader/JPMS layers/OSGi against current maintenance, Boot packaging, unload behavior, and security model.

### Phase 9: Policy, Security, Tenancy, Sandbox, and Observability Hardening

**Rationale:** Hooks should exist earlier, but production-grade enforcement needs real run/tool/provider/plugin/MCP flows to harden against. This is the gate before broader enterprise or multi-user deployment.

**Delivers:**
- Effective authority intersection: tenant ∩ user ∩ agent ∩ tool ∩ session/workspace ∩ approval.
- Deterministic policy engine with auditable decisions.
- Human approval pending state and approve/reject workflow.
- Secret storage/encryption/redaction improvements.
- Tenant isolation enforcement on queries, events, tools, providers, plugin visibility.
- Workspace/artifact constraints and sandbox boundary for risky tools.
- OTel trace export, metrics dashboards, structured logs, audit exports/event sinks.
- Prompt-injection/trust-label tests and gates for sensitive actions after untrusted content.

**Addresses:** Policy/security, human approval, tenant-aware execution, secrets, workspace/sandbox, observability, audit export, quotas/budgets.

**Avoids:** Full user/admin authority, destructive tools without approval, prompt-only guardrails, logs-only incident reconstruction, cross-tenant leaks.

**Research flag:** Needs deeper research for policy engine choice, secret manager/KMS integration, sandbox strategy, and tenancy/RBAC depth.

### Later Phases: Multi-Agent, Memory/RAG, Replay/Evals, MCP Server Export, CLI/TUI

**Rationale:** These are valuable extensions but should not shape v1 runtime boundaries. The v1 event/state/API design should make them possible without forcing them early.

**Delivers later:**
- Multi-agent handoffs/orchestration.
- Durable checkpoint replay/time-travel/debug and evaluation datasets.
- Memory/RAG/vector search with provenance, retention, sensitivity, deletion, and tenant controls.
- MCP server exporter exposing selected governed Pi tools/resources.
- CLI/TUI consuming the same REST/SSE APIs.
- Private plugin distribution and eventually marketplace-like features if strategy changes.

**Research flag:** Research when these become roadmap candidates; current guidance is to defer.

### Phase Ordering Rationale

- **Contracts before integrations:** runtime/event/tool/provider/extension contracts must be stable before MCP/PF4J/Vaadin-specific work.
- **Governance before external tools:** no MCP or plugin tools until the ToolExecutionGateway enforces policy, timeout, cancellation, audit, and observability.
- **Cloud API before GUI depth:** REST/SSE/read models should serve Admin GUI and future CLI/TUI equally.
- **SPI/Spring before dynamic plugins:** lower-risk extension paths validate the public API before classloader isolation and plugin lifecycle complexity.
- **MCP before/parallel with plugins only after tool governance:** both are external capability sources; both must normalize through registry/pipeline.
- **Hardening after flows exist but before broad production:** security/tenancy/sandbox/approval require real usage paths to test, but domain fields/hooks must exist from phase 1.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3: Model Provider Registry** — validate exact Spring AI 1.1.x artifacts, OpenAI-compatible streaming/tool-call semantics, provider usage accounting, and 2.x migration risk.
- **Phase 4: Governed Tool Pipeline** — validate JSON Schema library/versioning and policy decision record structure.
- **Phase 5: Admin GUI** — validate Vaadin Flow + Spring Security + SSE patterns if not already familiar.
- **Phase 7: MCP Client Bridge** — validate MCP Java SDK/Spring AI MCP maturity, transport scope, OAuth/protected-resource handling, and SSRF controls.
- **Phase 8: Dynamic Plugins** — compare PF4J/PF4J-Spring/custom classloader/JPMS/OSGi; test Spring Boot executable JAR packaging and unload behavior.
- **Phase 9: Policy/Security/Sandbox** — decide policy engine, secrets/KMS, tenancy/RBAC, sandbox/out-of-process strategy, and observability backend.

Phases with standard patterns (skip broad research-phase):
- **Phase 1: Runtime Spine** — standard ports/adapters and state-machine design; focus on internal schema decisions.
- **Phase 2: Cloud Server API/Persistence/SSE** — Spring Boot/PostgreSQL/SSE patterns are well documented; research only durability depth.
- **Phase 6: SPI/Spring Extension Surface** — standard ServiceLoader/Spring Boot starter patterns; validate packaging edge cases.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | MEDIUM-HIGH | Java 21, Spring Boot 3.5, Spring AI, MCP, PF4J, PostgreSQL, Micrometer/OTel, Resilience4j, Vaadin, and Testcontainers recommendations are backed by official/Context7 docs. Exact patch versions and some artifact names need validation at implementation time. |
| Features | HIGH | Runtime/session/tool/event/policy/observability table stakes are confirmed across LangGraph, Claude Managed Agents, Google Agent Runtime, OpenAI Agents SDK, Dify, Spring AI MCP, and MCP Runtime governance patterns. |
| Architecture | MEDIUM-HIGH | Ports-and-adapters modular monolith, framework-free runtime core, append-only events, governed tool pipeline, and MCP-as-adapter are strongly supported by dependency analysis and official MCP/Spring/OTel/PF4J docs. |
| Pitfalls | HIGH | SPI/Spring Boot/MCP auth pitfalls are official-doc backed; classloader/plugin and agent security practices are strong but some implementation details require project-specific validation. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Durability depth:** Decide whether v1 requires crash-resumable long-running runs or persisted history + safe cancellation only; this affects event store/checkpoint design.
- **Exact Spring AI/MCP versions:** Use Spring AI 1.1.x stable as baseline, but validate Maven artifacts, MCP module names, and 2.x migration path before coding.
- **MCP transport/auth scope:** Decide v1 mandatory transports and whether protected-resource OAuth is launch-blocking or gated to multi-tenant/protected tools.
- **Dynamic plugin framework:** Validate PF4J/PF4J-Spring/custom classloader options with Spring Boot executable JAR packaging, lifecycle, unload, and dependency-conflict tests.
- **Plugin security model:** Decide and document trusted in-process plugins vs untrusted out-of-process/containerized extensions; classloaders are not security sandboxes.
- **Policy engine choice:** Start with typed Java interface/default implementation; research OPA/Cedar/custom rules if authorization complexity grows.
- **Secret management:** Define `SecretRef` now; choose env/config-backed implementation first and Vault/KMS integration later.
- **Tenant/RBAC depth:** Include tenant IDs from day one even if single-tenant; decide how much RBAC is required for v1 Admin actions.
- **Sandbox/workspace implementation:** Define `Workspace` abstraction early; defer hardened code/shell execution unless it becomes a dedicated security phase.
- **Admin GUI stack details:** Vaadin is recommended for all-Java v1, but exact UI architecture and event streaming integration need implementation validation.
- **Observability backend:** Emit OTel/Micrometer from day one; choose collector/dashboard/log aggregation stack during deployment planning.

## Sources

### Primary (HIGH confidence)
- Spring AI reference via Context7 — model providers, ChatClient, structured output, tool/function calling, observability, MCP integration.
- Spring AI official release post, 2026-04-27 — Spring AI `1.0.6`, `1.1.5`, `2.0.0-M5`, Boot `3.5.14` upgrade, OpenAI SDK changes, MCP server improvements: https://spring.io/blog/2026/04/27/spring-ai-1-0-6-1-1-5-2-0-0-M5-available-now
- Spring Boot 3.5 Context7 docs — virtual threads, Actuator, observability, nested JAR/classloader behavior.
- MCP Java SDK / Spring AI MCP docs — sync/async clients, server capabilities, tools/resources/prompts, sampling, elicitation, progress, structured logging, stdio/SSE/Streamable HTTP transports: https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
- Model Context Protocol official architecture docs — host/client/server architecture, JSON-RPC lifecycle, tools/resources/prompts, transports: https://modelcontextprotocol.io/docs/concepts/architecture
- MCP Authorization specification 2025-06-18 — OAuth/protected-resource metadata, bearer token handling, audience/resource validation, resource indicators, HTTPS/PKCE, token passthrough prohibition: https://modelcontextprotocol.io/specification/2025-06-18/basic/authorization
- PostgreSQL current docs via Context7 — JSONB, full-text search, row-level security.
- Micrometer and OpenTelemetry docs — Observation API, traces/spans/OTLP exporters, Spring Boot observability integration.
- PF4J official docs via Context7 — plugin lifecycle, extension points, PluginManager, PluginClassLoader, runtime load/start/stop.
- Oracle Java `ServiceLoader` API docs — lazy/cached provider loading, classloader-specific discovery, reload behavior, provider errors: https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/ServiceLoader.html
- Claude Managed Agents official docs — agents/environments/sessions/events, SSE streaming, persistent event history, tools, MCP servers, sandbox options: https://platform.claude.com/docs/en/managed-agents/overview
- Google Cloud Gemini Enterprise Agent Platform Agent Runtime docs — sessions, memory, sandbox, observability, governance, identity, gateway, enterprise security: https://docs.cloud.google.com/gemini-enterprise-agent-platform/build/runtime
- OpenAI Agents SDK docs — runtime loop/state, models/providers, tools/MCP, guardrails, human review, observability/evals: https://platform.openai.com/docs/guides/agents
- Dify docs — app/workflow concepts, API/MCP publishing, tools, plugin/custom/workflow/MCP tools, knowledge grounding: https://docs.dify.ai/en/use-dify/getting-started/key-concepts
- OWASP Top 10 for LLMs and Gen AI Apps 2025 — risk taxonomy for prompt injection, excessive agency, sensitive information, and tool misuse: https://genai.owasp.org/llm-top-10/

### Secondary (MEDIUM confidence)
- LangChain4j Context7 docs — Java model/tool/memory/streaming/MCP examples and comparative ecosystem evidence.
- LangGraph documentation via Context7 — durable execution, persistence checkpoints, streaming, memory, human-in-loop, visibility.
- MCP Runtime docs — Kubernetes-native MCP deployment/governance, registry, gateway, per-tool policy, grants/sessions, audit/observability: https://docs.mcpruntime.org/
- AWS Bedrock AgentCore multi-tenant agent guidance — tenant context headers, scoped tools, quotas, gateway and ABAC patterns.
- WorkOS 2026 AI agent auth checklist — scoped/intersected authority, stop-on-auth-failure, full-context audit.
- Adevinta “Java plugins with isolating class loaders” — classloader isolation benefits and limits, trusted plugin warning.
- Nicolas Fränkel “Rediscovering Java ServiceLoader” — ServiceLoader strengths/limitations and Spring bridging cautions.
- Microsoft Agent Framework FIDES announcement — emerging information-flow label and deterministic policy pattern.

### Tertiary (LOW/MEDIUM confidence)
- Ecosystem search results for JamJet Runtime Java, AgentScope Java, and Spring AI Alibaba architecture posts — directional evidence only, not used for critical claims without stronger sources.

---
*Research completed: 2026-06-13*  
*Ready for roadmap: yes*
