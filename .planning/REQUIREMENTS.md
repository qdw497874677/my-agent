# Requirements: Pi Java Agent Platform

**Defined:** 2026-06-13  
**Core Value:** 让云上 Agent 能稳定接入和扩展模型、工具、插件、MCP、Memory、Workspace 与业务系统，并以统一、通用的 Runtime 运行、观测和治理。

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Runtime Core

- [x] **CORE-01**: Developer can define an Agent with instructions, model configuration, tool allowlist, policies, and runtime limits.
- [x] **CORE-02**: System represents execution with explicit Session, Run, Step, Message, ToolCall, ToolResult, and RunEvent models.
- [x] **CORE-03**: Agent Runtime can execute a General Agent loop that sends messages to a model, receives text/tool-call intents, executes tools, appends results, and continues until completion or failure.
- [x] **CORE-04**: Agent Runtime emits ordered, provider-neutral RunEvents with IDs, sequence numbers, timestamps, trace IDs, tenant/user/session/run/step context, event type, payload, and redaction metadata.
- [x] **CORE-05**: Agent Runtime supports run status transitions, cancellation, deadlines, max-step budgets, and terminal states for completed, failed, cancelled, and policy-blocked runs.
- [x] **CORE-06**: Core runtime contracts remain framework-independent and do not depend on Spring Boot, Vaadin, PF4J, MCP, or provider SDK types.
- [x] **CORE-07**: Core runtime supports multiple Agent interaction modes, including chat-style message input, task/run input, structured form input, tool-driven execution, and future workflow/planner execution, without treating chat transcript as the only state model.
- [x] **CORE-08**: Core runtime models artifacts, attachments, intermediate outputs, and external references separately from messages so non-chat Agents can produce and consume structured work products.
- [x] **CORE-09**: Codebase follows COLA layer boundaries where Adapter handles external protocols/UI, App orchestrates use cases, Domain owns runtime models/rules/gateways, and Infrastructure implements external systems without Domain depending outward.

### Cloud Server API

- [x] **CLOUD-01**: User can create an Agent Run through an authenticated REST API.
- [x] **CLOUD-02**: User can stream RunEvents from an Agent Run through SSE using the same event envelope persisted by the platform.
- [x] **CLOUD-03**: User can query run status, run detail, event history, step history, messages, tool calls, and terminal result through REST APIs.
- [x] **CLOUD-04**: User can cancel a running Agent Run through REST API and observe cancellation in run state and event stream.
- [x] **CLOUD-05**: Cloud Server exposes baseline authentication/security context, tenant/user placeholders, request correlation IDs, structured logs, and health endpoints.
- [x] **CLOUD-06**: Cloud Server stores run/session/event/audit state durably using a PostgreSQL-backed implementation with migrations.

### Model Providers

- [x] **MODEL-01**: Developer can register and resolve model providers through a provider registry using model IDs, provider IDs, capabilities, and credential references. Validated in Phase 3: provider registry contracts and `docs/phase-03-model-provider-contracts.md`.
- [x] **MODEL-02**: Platform provides an OpenAI-compatible streaming chat adapter usable by the General Agent loop. Validated in Phase 3: OpenAI-compatible `StreamingModelClient` and Cloud Server fake provider E2E.
- [x] **MODEL-03**: Model adapter normalizes text deltas, tool-call intents, finish reasons, usage/tokens, latency, and provider errors into platform events and records. Validated in Phase 3: fake provider contract tests and persisted `model.delta` replay.
- [x] **MODEL-04**: Provider configuration uses SecretRef/CredentialRef boundaries so raw secrets are not exposed in logs, prompts, events, or Admin GUI. Validated in Phase 3: secret resolver/redaction tests and no-key E2E secret absence assertions.
- [x] **MODEL-05**: Provider calls support timeout, cancellation, retry/rate-limit/circuit-breaker hooks, and provider contract tests. Validated in Phase 3: resilience policy hooks plus `OpenAiCompatibleProviderContractTest`.

### Governed Tools

- [x] **TOOL-01**: Developer can register tools with canonical ToolDescriptor metadata including name, description, input schema, output schema or type, provenance, version, scopes, risk level, side-effect classification, and timeout defaults. Validated in Phase 4: `ToolDescriptor`, built-in registrations, `/api/tools`, and `docs/phase-04-governed-tool-contracts.md`.
- [x] **TOOL-02**: All tool calls from built-in tools, SPI tools, Spring Bean tools, dynamic plugin tools, and MCP tools execute through one ToolExecutionGateway. Validated in Phase 4: `GeneralAgentLoop` gateway routing and `CloudServerGovernedToolE2ETest` product-path execution.
- [x] **TOOL-03**: ToolExecutionGateway validates arguments against schema before execution and normalizes success/failure results after execution. Validated in Phase 4: gateway orchestration, Networknt validator boundary, and focused gateway/E2E tests.
- [x] **TOOL-04**: ToolExecutionGateway enforces timeout, cancellation, max payload limits, error classification, redaction, and result summarization hooks. Validated in Phase 4: payload limiter/redactor implementations and `GovernedToolSecurityRedactionE2ETest`.
- [x] **TOOL-05**: ToolExecutionGateway invokes ToolPolicy before execution and can allow, deny, require approval, require sandbox, or block a tool call. Validated in Phase 4: default policy evaluator plus deny/approval Cloud Server E2E.
- [x] **TOOL-06**: Platform records audit entries and RunEvents for tool call proposed, policy decided, started, updated, completed, failed, denied, and cancelled states. Validated in Phase 4: `tool.lifecycle` event DTOs, in-memory/JDBC audit path, and Cloud Server E2E event/audit assertions.
- [x] **TOOL-07**: v1 includes safe built-in example tools that demonstrate read-only and side-effectful classifications without unrestricted shell/file access. Validated in Phase 4: `builtin.info`, `builtin.workspace.write`, `builtin.workspace.command`, and bounded local-temp documentation.

### Workspace and Resources

- [x] **WORK-01**: Domain defines Workspace, WorkspaceSession, WorkspaceScope, WorkspaceSnapshot, Artifact, Attachment, and Resource/Mount abstractions as first-class runtime concepts.
- [x] **WORK-02**: WorkspaceGateway abstracts file/resource/artifact operations without exposing host filesystem assumptions to Domain.
- [x] **WORK-03**: CommandExecutionGateway executes commands inside a Workspace boundary rather than directly on the host process environment. Validated in Phase 4: `AllowlistedCommandExecutionGateway`, `WorkspaceCommandTool`, and workspace-bound command E2E.
- [x] **WORK-04**: ToolContext and RunContext include workspaceId and session/resource scope so tool execution can be constrained per Run.
- [x] **WORK-05**: Workspace supports snapshot/restore contracts and leaves room for fingerprint/drift detection and replay-safe execution.
- [ ] **WORK-06**: Workspace and Resource providers can be extended via SPI, Spring, plugins, and MCP-backed adapters without bypassing ToolExecutionGateway.
- [x] **WORK-07**: v1 may provide fake or local-temp workspace implementations for tests, but does not expose unrestricted host shell/filesystem as the default execution model. Validated in Phase 4: local-temp workspace limitations and allowlisted command-only built-in.
- [x] **WORK-08**: Platform can estimate command/tool impact through a provision/preview contract before executing side-effectful workspace actions. Validated in Phase 4: `ProvisionPreview`, preview events/audit, and approval-required no-execution E2E.

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

### Agent Web Console and Admin Governance

- [x] **GUI-01**: User can view an Agent Catalog listing available Agents with name, description, capabilities, allowed tools, risk indicators, and entry actions. Validated in Phase 5: `/api/agents`, catalog cards, browser E2E, and `docs/phase-05-web-console.md`.
- [x] **GUI-02**: User can enter an Agent interaction page, use a chat-style input for v1, receive streaming model output, and see the current Run status while the API remains open to non-chat input modes. Validated in Phase 5: Console route/action plans, model event assertions, and Playwright no-key E2E.
- [x] **GUI-03**: User can see tool calls as execution cards showing tool name, status, purpose, risk/side-effect label, progress, redacted result summary, and errors. Validated in Phase 5: tool lifecycle cards and browser E2E `tool.*` event assertions.
- [x] **GUI-04**: User can view and continue Session history, including past Runs, messages, tool calls, and terminal results. Validated in Phase 5: public session history/continuation API paths and browser E2E continuation branch.
- [x] **GUI-05**: User can cancel a running Agent Run from the Web Console. Validated in Phase 5: Console cancellation plan and browser E2E cancellation branch.
- [x] **GUI-06**: User or Admin can approve or reject gated tool calls through an approval card when ToolPolicy requires approval. Validated in Phase 5: approval DTO/API/cards and Playwright approve/reject branches.
- [x] **GUI-07**: Admin can inspect runtime governance views for provider configuration/status, extension status, MCP server status, plugin status, tool registry health, policy decisions, and audit records. Validated in Phase 5: Admin Governance APIs/views and browser E2E overview/policy/audit checks.
- [x] **GUI-08**: Web GUI uses public REST/SSE/read-model APIs rather than private runtime or database access. Validated in Phase 5: `ConsoleHttpClient`, `EventStreamClient`, public DTO anchors, and browser E2E API consumption.

### Observability, Policy, and Security

- [ ] **OPS-01**: Platform emits structured logs, metrics, and OpenTelemetry-compatible trace/span hooks for runs, model calls, tool calls, MCP calls, plugin lifecycle, and policy decisions.
- [x] **OPS-02**: Platform stores audit records for security-sensitive actions including run creation/cancellation, tool decisions, provider credential usage, plugin changes, and MCP calls. Validated in Phase 4: tool decision/preview/execution audit assertions in Cloud Server E2E.
- [x] **OPS-03**: Platform includes a default policy engine implementation and a pluggable policy interface for future RBAC/ABAC/quota/compliance checks. Validated in Phase 4: `DefaultToolPolicyEvaluator` and App `ToolPolicyEvaluator` port.
- [x] **OPS-04**: Platform models tenant ID, user ID, session ID, run ID, workspace ID, and trace ID in runtime context even if v1 runs single-tenant.
- [x] **OPS-05**: Platform prevents raw secrets and sensitive payloads from being displayed in Web Console, Admin Governance views, logs, prompts, and default persisted events. Validated in Phase 4: `GovernedToolSecurityRedactionE2ETest` fake-secret absence checks across REST, RunEvents, audit, and persisted payload strings.
- [x] **OPS-06**: Platform exposes testkit utilities including fake model providers, fake tools, fake policies, and conformance tests for extensions.

### End-to-End Verification

- [x] **E2E-01**: Platform provides a headless E2E test harness that can create an Agent Run through API/runtime entry points, stream events, persist state, and assert terminal status without real model keys.
- [x] **E2E-02**: Headless E2E verifies the successful model-to-tool-to-model loop using FakeModelProvider and FakeTool through ToolExecutionGateway. Validated in Phase 4: `CloudServerGovernedToolE2ETest` safe read-only and workspace command success paths.
- [x] **E2E-03**: Headless E2E verifies ToolPolicy deny and require-approval paths, including event stream, audit records, and prevention of unauthorized tool execution. Validated in Phase 4: `CloudServerGovernedToolE2ETest` deny and approval-required no-side-effect paths.
- [x] **E2E-04**: Headless E2E verifies cancellation, timeout, max-step, terminal events, and absence of hanging model/tool tasks.
- [x] **E2E-05**: Headless E2E verifies SSE ordering, terminal events, and reconnect/replay behavior using event sequence or lastEventId.
- [x] **E2E-06**: Security E2E verifies raw secrets and sensitive payloads do not appear in API responses, RunEvents, audit records, logs, or Web Console views by default. Validated in Phase 4: `GovernedToolSecurityRedactionE2ETest` covers default API/event/audit/persisted payload paths; Web Console display consumes the same redacted APIs in Phase 5.
- [x] **E2E-07**: Browser E2E verifies Agent Catalog, Agent interaction page, streaming output, tool cards, approval cards, session history, cancel action, and basic governance views. Validated in Phase 5: Playwright `e2e/phase-05-web-console.spec.ts` using no-key fake runtime fixtures.
- [ ] **E2E-08**: Integration E2E verifies Fake MCP server discovery/execution and sample plugin JAR loading/disable flows through the same ToolExecutionGateway, policy, audit, and event pipeline.

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Product Clients

- **CLIENT-01**: User can operate the platform through a dedicated TUI client that consumes the same REST/SSE API as Web Console.
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
| Full TUI/CLI in v1 | Cloud Server and Web Console are the first product surfaces; future clients must use public APIs. |
| Dify-style visual workflow builder in v1 | Different product category and high UI complexity; v1 focuses on Agent Catalog, Chat/Run entry, runtime cockpit, and extension governance. |
| Complex Agent Studio in v1 | Full visual agent authoring/versioning/publishing can wait; v1 needs basic Agent entry and run experience first. |
| Full plugin marketplace in v1 | Marketplace requires trust, distribution, review, billing, moderation, and ecosystem operations. |
| Unrestricted shell/file/code execution in v1 | Unsafe for cloud by default; require workspace, sandbox, policy, approval, and audit before enabling. |
| Every model provider in v1 | OpenAI-compatible first validates provider abstraction; other providers come through adapters later. |
| Guaranteed hot unload of JVM plugins in v1 | JVM classloader unload/resource cleanup is complex; support load/disable/quarantine first. |
| Heavy RAG product in v1 | Memory/Retrieval extension boundaries are sufficient until runtime and tool governance are stable. |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| CORE-01 | Phase 1 | Complete |
| CORE-02 | Phase 1 | Complete |
| CORE-03 | Phase 1 | Complete |
| CORE-04 | Phase 1 | Complete |
| CORE-05 | Phase 1 | Complete |
| CORE-06 | Phase 1 | Complete |
| CORE-07 | Phase 1 | Complete |
| CORE-08 | Phase 1 | Complete |
| CORE-09 | Phase 1 | Complete |
| CLOUD-01 | Phase 2 | Complete |
| CLOUD-02 | Phase 2 | Complete |
| CLOUD-03 | Phase 2 | Complete |
| CLOUD-04 | Phase 2 | Complete |
| CLOUD-05 | Phase 2 | Complete |
| CLOUD-06 | Phase 2 | Complete |
| MODEL-01 | Phase 3 | Complete — validated by provider registry contracts and `docs/phase-03-model-provider-contracts.md` |
| MODEL-02 | Phase 3 | Complete — validated by OpenAI-compatible streaming adapter and fake Cloud Server provider E2E |
| MODEL-03 | Phase 3 | Complete — validated by normalization contract tests and persisted `model.delta` replay |
| MODEL-04 | Phase 3 | Complete — validated by CredentialRef/SecretRef redaction tests and no-key E2E secret absence checks |
| MODEL-05 | Phase 3 | Complete — validated by timeout/cancellation/resilience contract tests |
| WORK-01 | Phase 1 | Complete |
| WORK-02 | Phase 1 | Complete |
| WORK-03 | Phase 4 | Complete |
| WORK-04 | Phase 1 | Complete |
| WORK-05 | Phase 1 | Complete |
| WORK-06 | Phase 6 | Pending |
| WORK-07 | Phase 4 | Complete |
| WORK-08 | Phase 4 | Complete |
| TOOL-01 | Phase 4 | Complete |
| TOOL-02 | Phase 4 | Complete |
| TOOL-03 | Phase 4 | Complete |
| TOOL-04 | Phase 4 | Complete |
| TOOL-05 | Phase 4 | Complete |
| TOOL-06 | Phase 4 | Complete |
| TOOL-07 | Phase 4 | Complete |
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
| GUI-01 | Phase 5 | Complete — validated by `/api/agents`, catalog cards, browser E2E, and `docs/phase-05-web-console.md` |
| GUI-02 | Phase 5 | Complete — validated by Console route/action plans, model event assertions, and no-key Playwright E2E |
| GUI-03 | Phase 5 | Complete — validated by tool lifecycle cards and browser E2E `tool.*` event assertions |
| GUI-04 | Phase 5 | Complete — validated by session history/continuation API paths and browser E2E continuation branch |
| GUI-05 | Phase 5 | Complete — validated by Console cancellation plan and browser E2E cancellation branch |
| GUI-06 | Phase 5 | Complete — validated by approval DTO/API/cards and Playwright approve/reject branches |
| GUI-07 | Phase 5 | Complete — validated by Admin Governance APIs/views and browser E2E overview/policy/audit checks |
| GUI-08 | Phase 5 | Complete — validated by public REST/SSE/read-model client boundaries and browser E2E API consumption |
| OPS-01 | Phase 9 | Pending |
| OPS-02 | Phase 4 | Complete |
| OPS-03 | Phase 4 | Complete |
| OPS-04 | Phase 1 | Complete |
| OPS-05 | Phase 4 | Complete |
| OPS-06 | Phase 1 | Complete |
| E2E-01 | Phase 2 | Complete |
| E2E-02 | Phase 4 | Complete |
| E2E-03 | Phase 4 | Complete |
| E2E-04 | Phase 2 | Complete |
| E2E-05 | Phase 2 | Complete |
| E2E-06 | Phase 4 | Complete — validated by `GovernedToolSecurityRedactionE2ETest` fake-secret absence checks across REST, event history, persisted RunEvents, audit records, and safe exception paths |
| E2E-07 | Phase 5 | Complete — validated by Playwright `e2e/phase-05-web-console.spec.ts` using no-key fake runtime fixtures |
| E2E-08 | Phase 7, Phase 8 | Pending |

**Coverage:**
- v1 requirements: 75 total
- Mapped to phases: 75
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-13*
*Last updated: 2026-06-15 after Phase 5 Web Console browser E2E completion*
