# Requirements: Pi Java Agent Platform

**Defined:** 2026-06-13  
**Core Value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一 Runtime 运行、观测和治理。

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Runtime Core

- [ ] **CORE-01**: Developer can define an Agent with instructions, model configuration, tool allowlist, policies, and runtime limits.
- [ ] **CORE-02**: System represents execution with explicit Session, Run, Step, Message, ToolCall, ToolResult, and RunEvent models.
- [ ] **CORE-03**: Agent Runtime can execute a General Agent loop that sends messages to a model, receives text/tool-call intents, executes tools, appends results, and continues until completion or failure.
- [ ] **CORE-04**: Agent Runtime emits ordered, provider-neutral RunEvents with IDs, sequence numbers, timestamps, trace IDs, tenant/user/session/run/step context, event type, payload, and redaction metadata.
- [ ] **CORE-05**: Agent Runtime supports run status transitions, cancellation, deadlines, max-step budgets, and terminal states for completed, failed, cancelled, and policy-blocked runs.
- [ ] **CORE-06**: Core runtime contracts remain framework-independent and do not depend on Spring Boot, Vaadin, PF4J, MCP, or provider SDK types.

### Cloud Server API

- [ ] **CLOUD-01**: User can create an Agent Run through an authenticated REST API.
- [ ] **CLOUD-02**: User can stream RunEvents from an Agent Run through SSE using the same event envelope persisted by the platform.
- [ ] **CLOUD-03**: User can query run status, run detail, event history, step history, messages, tool calls, and terminal result through REST APIs.
- [ ] **CLOUD-04**: User can cancel a running Agent Run through REST API and observe cancellation in run state and event stream.
- [ ] **CLOUD-05**: Cloud Server exposes baseline authentication/security context, tenant/user placeholders, request correlation IDs, structured logs, and health endpoints.
- [ ] **CLOUD-06**: Cloud Server stores run/session/event/audit state durably using a PostgreSQL-backed implementation with migrations.

### Model Providers

- [ ] **MODEL-01**: Developer can register and resolve model providers through a provider registry using model IDs, provider IDs, capabilities, and credential references.
- [ ] **MODEL-02**: Platform provides an OpenAI-compatible streaming chat adapter usable by the General Agent loop.
- [ ] **MODEL-03**: Model adapter normalizes text deltas, tool-call intents, finish reasons, usage/tokens, latency, and provider errors into platform events and records.
- [ ] **MODEL-04**: Provider configuration uses SecretRef/CredentialRef boundaries so raw secrets are not exposed in logs, prompts, events, or Admin GUI.
- [ ] **MODEL-05**: Provider calls support timeout, cancellation, retry/rate-limit/circuit-breaker hooks, and provider contract tests.

### Governed Tools

- [ ] **TOOL-01**: Developer can register tools with canonical ToolDescriptor metadata including name, description, input schema, output schema or type, provenance, version, scopes, risk level, side-effect classification, and timeout defaults.
- [ ] **TOOL-02**: All tool calls from built-in tools, SPI tools, Spring Bean tools, dynamic plugin tools, and MCP tools execute through one ToolExecutionGateway.
- [ ] **TOOL-03**: ToolExecutionGateway validates arguments against schema before execution and normalizes success/failure results after execution.
- [ ] **TOOL-04**: ToolExecutionGateway enforces timeout, cancellation, max payload limits, error classification, redaction, and result summarization hooks.
- [ ] **TOOL-05**: ToolExecutionGateway invokes ToolPolicy before execution and can allow, deny, require approval, require sandbox, or block a tool call.
- [ ] **TOOL-06**: Platform records audit entries and RunEvents for tool call proposed, policy decided, started, updated, completed, failed, denied, and cancelled states.
- [ ] **TOOL-07**: v1 includes safe built-in example tools that demonstrate read-only and side-effectful classifications without unrestricted shell/file access.

### Extension Fabric

- [ ] **EXT-01**: Developer can extend the platform through Java SPI for tools, model providers, policies, event sinks, memory providers, workspace providers, and extension metadata.
- [ ] **EXT-02**: Spring Boot applications can register tools, providers, policies, and event listeners through Spring Beans or annotations without modifying runtime core.
- [ ] **EXT-03**: Platform exposes a public extension API/JAR with compatibility/version metadata, lifecycle states, health status, and conformance tests.
- [ ] **EXT-04**: Admin can view extension sources, registered capabilities, health, compatibility, enable/disable status, and errors.
- [ ] **EXT-05**: Extension loading never bypasses ToolExecutionGateway, Policy, Audit, Event, and CredentialRef boundaries.

### MCP Integration

- [ ] **MCP-01**: Admin can configure trusted MCP servers for the platform to connect to as an MCP client.
- [ ] **MCP-02**: Platform can discover MCP tools, normalize their schemas into ToolDescriptor, and register them with provenance and server health metadata.
- [ ] **MCP-03**: Platform executes MCP tool calls only through ToolExecutionGateway with policy, timeout, cancellation, audit, redaction, and events.
- [ ] **MCP-04**: Platform records MCP connection state, discovery errors, invocation errors, auth failures, and server health in Admin GUI and event/audit records.
- [ ] **MCP-05**: MCP integration includes security boundaries for server allowlists, credential references, transport configuration, and SSRF-sensitive network controls.

### Dynamic Plugins

- [ ] **PLUG-01**: Admin can configure a controlled plugin directory for trusted dynamic plugin JARs.
- [ ] **PLUG-02**: Platform can load plugin descriptors, validate platform/API compatibility, and register plugin-provided capabilities through the same extension registry.
- [ ] **PLUG-03**: Platform tracks plugin lifecycle states including discovered, loaded, started, disabled, failed, and quarantined.
- [ ] **PLUG-04**: Admin can view plugin metadata, registered capabilities, health, load errors, and compatibility errors in the Admin GUI.
- [ ] **PLUG-05**: Admin can disable or quarantine a plugin so its capabilities are unavailable for new runs.
- [ ] **PLUG-06**: v1 explicitly treats dynamic plugin isolation as lifecycle/dependency isolation, not a security sandbox for untrusted code.

### Admin GUI

- [ ] **GUI-01**: Admin can view a list of Agent Runs with status, timestamps, model, agent definition, token/usage summary when available, and terminal state.
- [ ] **GUI-02**: Admin can inspect a Run timeline showing ordered RunEvents, steps, model events, tool calls, policy decisions, errors, and terminal result.
- [ ] **GUI-03**: Admin can inspect tool calls with descriptor metadata, provenance, policy decision, redacted input/output summaries, duration, status, and error details.
- [ ] **GUI-04**: Admin can view provider configuration/status, extension status, MCP server status, plugin status, and tool registry health.
- [ ] **GUI-05**: Admin can cancel a running Agent Run from the GUI.
- [ ] **GUI-06**: Admin GUI uses public REST/SSE/read-model APIs rather than private runtime access.

### Observability, Policy, and Security

- [ ] **OPS-01**: Platform emits structured logs, metrics, and OpenTelemetry-compatible trace/span hooks for runs, model calls, tool calls, MCP calls, plugin lifecycle, and policy decisions.
- [ ] **OPS-02**: Platform stores audit records for security-sensitive actions including run creation/cancellation, tool decisions, provider credential usage, plugin changes, and MCP calls.
- [ ] **OPS-03**: Platform includes a default policy engine implementation and a pluggable policy interface for future RBAC/ABAC/quota/compliance checks.
- [ ] **OPS-04**: Platform models tenant ID, user ID, session ID, run ID, workspace ID, and trace ID in runtime context even if v1 runs single-tenant.
- [ ] **OPS-05**: Platform prevents raw secrets and sensitive payloads from being displayed in Admin GUI, logs, prompts, and default persisted events.
- [ ] **OPS-06**: Platform exposes testkit utilities including fake model providers, fake tools, fake policies, and conformance tests for extensions.

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Product Clients

- **CLIENT-01**: User can operate the platform through a dedicated TUI client that consumes the same REST/SSE API as Admin GUI.
- **CLIENT-02**: User can operate the platform through a CLI client for local developer workflows.
- **CLIENT-03**: User can run in local mode without Cloud Server for developer testing.

### Advanced Agent Capabilities

- **ADV-01**: Platform supports multi-agent handoffs and orchestrated agent workflows.
- **ADV-02**: Platform supports durable crash-resumable execution and replay from checkpoints.
- **ADV-03**: Platform supports evaluation datasets, regression replay, and model/tool quality scoring.
- **ADV-04**: Platform supports human approval queues with assignment, timeout, and audit workflow.

### Knowledge and Workspace

- **KNOW-01**: Platform provides full RAG/knowledge-base ingestion, indexing, permissions, and retrieval UI.
- **KNOW-02**: Platform supports vector memory with pgvector or external vector stores.
- **WORK-01**: Platform provides sandboxed code/file/shell workspace execution for Coding Agent scenarios.
- **WORK-02**: Platform supports Git repository checkout, patch, diff, tests, and artifact publishing.

### Ecosystem

- **ECO-01**: Platform provides a plugin marketplace with distribution, trust review, ratings, versioning, and install workflows.
- **ECO-02**: Platform can expose its own tools/agents as an MCP server or gateway.
- **ECO-03**: Platform supports full multi-tenant RBAC administration, quotas, billing, and tenant-specific catalogs.

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Direct TypeScript pi code port | The product is a Java cloud platform; pi is a reference design, not the implementation target. |
| Full TUI/CLI in v1 | Cloud Server and Admin GUI are the first product surfaces; future clients must use public APIs. |
| Dify-style visual workflow builder in v1 | Different product category and high UI complexity; v1 focuses on runtime cockpit and extension governance. |
| Full plugin marketplace in v1 | Marketplace requires trust, distribution, review, billing, moderation, and ecosystem operations. |
| Unrestricted shell/file/code execution in v1 | Unsafe for cloud by default; require workspace, sandbox, policy, approval, and audit before enabling. |
| Every model provider in v1 | OpenAI-compatible first validates provider abstraction; other providers come through adapters later. |
| Guaranteed hot unload of JVM plugins in v1 | JVM classloader unload/resource cleanup is complex; support load/disable/quarantine first. |
| Heavy RAG product in v1 | Memory/Retrieval extension boundaries are sufficient until runtime and tool governance are stable. |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CORE-01 | Phase 1 | Pending |
| CORE-02 | Phase 1 | Pending |
| CORE-03 | Phase 1 | Pending |
| CORE-04 | Phase 1 | Pending |
| CORE-05 | Phase 1 | Pending |
| CORE-06 | Phase 1 | Pending |
| CLOUD-01 | Phase 2 | Pending |
| CLOUD-02 | Phase 2 | Pending |
| CLOUD-03 | Phase 2 | Pending |
| CLOUD-04 | Phase 2 | Pending |
| CLOUD-05 | Phase 2 | Pending |
| CLOUD-06 | Phase 2 | Pending |
| MODEL-01 | Phase 3 | Pending |
| MODEL-02 | Phase 3 | Pending |
| MODEL-03 | Phase 3 | Pending |
| MODEL-04 | Phase 3 | Pending |
| MODEL-05 | Phase 3 | Pending |
| TOOL-01 | Phase 4 | Pending |
| TOOL-02 | Phase 4 | Pending |
| TOOL-03 | Phase 4 | Pending |
| TOOL-04 | Phase 4 | Pending |
| TOOL-05 | Phase 4 | Pending |
| TOOL-06 | Phase 4 | Pending |
| TOOL-07 | Phase 4 | Pending |
| EXT-01 | Phase 6 | Pending |
| EXT-02 | Phase 6 | Pending |
| EXT-03 | Phase 6 | Pending |
| EXT-04 | Phase 6 | Pending |
| EXT-05 | Phase 6 | Pending |
| MCP-01 | Phase 7 | Pending |
| MCP-02 | Phase 7 | Pending |
| MCP-03 | Phase 7 | Pending |
| MCP-04 | Phase 7 | Pending |
| MCP-05 | Phase 7 | Pending |
| PLUG-01 | Phase 8 | Pending |
| PLUG-02 | Phase 8 | Pending |
| PLUG-03 | Phase 8 | Pending |
| PLUG-04 | Phase 8 | Pending |
| PLUG-05 | Phase 8 | Pending |
| PLUG-06 | Phase 8 | Pending |
| GUI-01 | Phase 5 | Pending |
| GUI-02 | Phase 5 | Pending |
| GUI-03 | Phase 5 | Pending |
| GUI-04 | Phase 5 | Pending |
| GUI-05 | Phase 5 | Pending |
| GUI-06 | Phase 5 | Pending |
| OPS-01 | Phase 9 | Pending |
| OPS-02 | Phase 4 | Pending |
| OPS-03 | Phase 4 | Pending |
| OPS-04 | Phase 1 | Pending |
| OPS-05 | Phase 4 | Pending |
| OPS-06 | Phase 1 | Pending |

**Coverage:**
- v1 requirements: 54 total
- Mapped to phases: 54
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-13*
*Last updated: 2026-06-13 after roadmap creation*
