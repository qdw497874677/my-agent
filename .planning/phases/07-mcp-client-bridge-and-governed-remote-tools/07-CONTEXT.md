# Phase 7: MCP Client Bridge and Governed Remote Tools - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 7 adds MCP as a trusted remote tool adapter for Pi. It must configure trusted MCP servers, discover MCP tools, normalize them into Pi `ToolDescriptor` records, register executable bindings behind the existing governed tool pipeline, and surface MCP connection/discovery/invocation/auth/health status in Admin Governance. MCP tools must execute only through `ToolExecutionGateway` with existing policy, timeout, cancellation, audit, redaction, and event semantics.

This phase does **not** implement Pi as an MCP server, MCP resources/prompts/sampling/elicitation, full OAuth DCR/Protected Resource Metadata flows, dynamic hot add/edit/delete of MCP servers from Admin UI, a general remote-code sandbox, or dynamic plugin JAR loading.

</domain>

<decisions>
## Implementation Decisions

### Configuration and Governance Boundary
- **D-01:** Phase 7 should use **configuration-file-first MCP server setup**. Trusted MCP servers are defined through Spring/YAML/env typed configuration as the Phase 7 authoritative source. This keeps the first MCP bridge aligned with the existing inspect-only Admin Governance style from Phases 5 and 6.
- **D-02:** Downstream planning may introduce a repository abstraction such as `McpServerConfigRepository` and reserve a DB-backed source for future dynamic management, but Phase 7 must not build full Admin CRUD for adding/editing/deleting MCP servers.
- **D-03:** Admin Governance for MCP in Phase 7 is **read-only plus refresh discovery**. Admin can inspect configured servers, transport/auth metadata, discovery status, tool count, health, and redacted errors; Admin may trigger a low-risk refresh/rediscovery action. Admin must not perform full server configuration mutation in this phase.
- **D-04:** If any DB schema or app port is added for MCP configuration, treat it as a future-proof seam or minimal backend foundation, not a product-visible dynamic configuration UI.

### Transport and Authentication Scope
- **D-05:** Phase 7 v1 should support the full practical transport set: Streamable HTTP/stateless HTTP, stdio, and legacy SSE compatibility. Streamable HTTP should be the recommended/default remote transport. Stdio and legacy SSE must be explicit configuration choices, not accidental defaults.
- **D-06:** Stdio support must be constrained to configured commands/server definitions. It is for trusted configured MCP servers only; it must not become a general shell/process execution feature.
- **D-07:** Authentication in Phase 7 should be **static credentials first**: `CredentialRef`/`SecretRef` driven API key, Bearer token, and custom header injection. Raw secrets must be resolved only inside the MCP infrastructure adapter and must not appear in public DTOs, events, audit, logs, errors, or Admin UI.
- **D-08:** Full OAuth 2.1, Protected Resource Metadata, Dynamic Client Registration, and scope step-up flows are deferred. Planning may leave extension points for future token providers, but must not make community OAuth/DCR modules part of the Phase 7 critical path.
- **D-09:** Phase 7 covers **MCP Tools only**. MCP resources, prompts, sampling, elicitation, and Pi-as-MCP-server capabilities are deferred unless a future phase explicitly promotes them.

### Registration Model and Descriptor Normalization
- **D-10:** MCP should use an **independent MCP registry/adapter model**, not represent remote MCP servers as Java `ExtensionSource` implementations. A dedicated `McpServerRegistry`/`McpToolRegistry` or equivalent may own remote connection, discovery, health, and tool binding state.
- **D-11:** Although MCP has an independent registry, its governance read model should reuse the Phase 6 source/capability/provenance/health language so Admin Governance remains consistent across SPI, Spring, MCP, and future plugin sources.
- **D-12:** MCP tools must normalize into the existing descriptor-first tool model: `ToolDescriptor` plus `ToolExecutorBinding`. No MCP-specific execution path may bypass `ToolRegistry` and `ToolExecutionGateway`.
- **D-13:** Global MCP tool IDs should use a server-qualified naming convention such as `mcp.<serverId>.<toolName>` or `<serverId>.<toolName>`. The exact prefix is planner discretion, but IDs must avoid collisions across multiple MCP servers and remain clear in audit/events/Admin UI.
- **D-14:** MCP `inputSchema` should be passed through as JSON Schema into Pi `ToolSchema` with `dialect=json-schema` or an equivalent explicit dialect marker. Do not build a custom Pi schema conversion layer in Phase 7. Discovery should preserve useful MCP metadata and annotations for policy/governance hints.

### Security Defaults and Policy Semantics
- **D-15:** Configured MCP servers are treated as trusted for connection purposes, but Phase 7 must retain minimum safety checks: only configured endpoints/commands are used, dangerous URL schemes are rejected, redirects/headers must not leak credentials to unconfigured hosts, stdio commands must come from trusted configuration, and all connection/discovery errors are sanitized.
- **D-16:** Tool execution remains conservative. Discovered/visible MCP tools are not automatically executable. An Agent must explicitly allow the relevant MCP server/tool scope before `ToolPolicyEvaluator` can allow execution.
- **D-17:** MCP tools should default to remote/external risk metadata. If MCP annotations indicate `readOnlyHint=true`, policy may allow direct execution when the Agent allowlist permits it. Non-read-only, destructive, open-world, or unknown MCP tools should default toward preview/approval or denial according to existing policy rules.
- **D-18:** MCP tool policy decisions must continue to use existing meanings: allow, deny, require approval, require sandbox, and block. Approval/sandbox gates must prevent MCP remote invocation until the gateway permits execution.
- **D-19:** `CredentialRef`/`SecretRef` and redaction rules from prior phases are mandatory for all MCP server auth, headers, errors, invocation arguments, results, Admin status, and audit details.

### Health, Discovery, and Failure Experience
- **D-20:** MCP connection/discovery should run at startup and through an Admin-triggered manual refresh. Phase 7 should not require complex periodic polling unless planning finds a simple, low-risk health refresh seam.
- **D-21:** If an MCP server is down or discovery fails, Admin Governance should retain a visible server status with sanitized error details. Previously known/configured server entries should not disappear silently.
- **D-22:** Tools for an unhealthy server should be represented as unavailable rather than silently hidden. Registry resolution/execution should fail safely with a normalized unavailable/connection/auth/timeout error and should still pass through the gateway-visible/auditable path where possible.
- **D-23:** MCP invocation failures, auth failures, timeouts, and server unavailable states should primarily map to standard governed tool outcomes such as `tool.failed`, `tool.denied`, or normalized policy/server-unavailable errors. Phase 7 should not create a large separate public `mcp.*` event family unless planning proves it is necessary; Admin MCP status can carry MCP-specific diagnostics.
- **D-24:** Events and audit for MCP failures must expose only redacted summaries, server/tool references, status/category, and user-action hints. Raw remote error bodies, credentials, request headers, and sensitive result payloads must not be persisted or shown by default.

### Verification and Test Matrix
- **D-25:** Phase 7 E2E must prove Fake MCP server discovery and tool execution through the existing `ToolExecutionGateway`, policy, audit, event, and redaction pipeline. It should mirror the Phase 6 extension conformance E2E and Phase 4 governed tool E2E patterns.
- **D-26:** Default verification must be no-key and deterministic. Use fake/loopback MCP servers and fake model runtime fixtures; do not require real MCP services, model keys, external credentials, or Docker-only infrastructure for normal local/CI verification.
- **D-27:** Tests should cover at least: successful discovery, server-qualified tool naming, schema passthrough, allowed read-only call, blocked/approval-required call, server down/discovery failure, auth failure with redaction, timeout/cancellation, stdio trusted-command path, HTTP transport path, and Admin refresh/status rendering.
- **D-28:** Architecture gates must ensure MCP SDK/Spring AI MCP dependencies stay in the MCP infrastructure/adapter module. Domain, App, extension API, client DTOs, and Spring starter public APIs must not leak `io.modelcontextprotocol.*` or Spring AI MCP types.

### Folded Todos
- No pending todos matched Phase 7 scope.

### the agent's Discretion
- Exact MCP module names, class names, package structure, and whether Spring AI MCP or direct MCP Java SDK is the primary adapter are planner/researcher discretion, provided MCP types remain outside Domain/App/client contracts.
- Exact Admin endpoint paths and DTO names for MCP status/refresh are planner discretion, but they must use public `pi-agent-client`/Adapter DTO boundaries and stay read-only plus refresh.
- Exact resilience defaults, timeout values, and refresh implementation details are planner discretion, but they must preserve cancellation, redaction, audit, and no-key testability.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 7 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 7 — Phase goal, MCP-01..MCP-05 and E2E-08 success criteria, UI hint, and research-needed topics for MCP Java SDK/Spring AI MCP, transports, auth, and SSRF controls.
- `.planning/REQUIREMENTS.md` §MCP Integration — MCP-01 through MCP-05 requirements for trusted server configuration, discovery, descriptor normalization, governed execution, health/error visibility, and transport/security boundaries.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E-08 requirement for Fake MCP server discovery/execution through the governed tool pipeline.
- `.planning/PROJECT.md` — Product constraints: Java-first, COLA boundaries, cloud safety, Workspace boundary, extensibility, verification, and v1 extension integration value.
- `.planning/STATE.md` — Current Phase 7 state and accumulated Phase 1-6 implementation decisions.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Framework-free Domain, event taxonomy, tool/policy/workspace ports, and strict architecture gates.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Public DTO boundary, persist-then-emit events, audit/persistence ports, dev auth + JWT-ready security, and composition-root patterns.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — CredentialRef/SecretRef, SecretResolver, provider adapter isolation, resilience hooks, and no-key contract testing patterns.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Descriptor-first `ToolRegistry`, mandatory `ToolExecutionGateway`, policy/preview/approval/sandbox semantics, tool lifecycle events, audit, payload limits, and redaction.
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Admin Governance public API/UI boundaries, read-only governance tradition, tool cards, approval semantics, and MCP placeholder deferral.
- `.planning/phases/06-java-extension-surface-spi-and-spring/06-CONTEXT.md` — Extension source/capability/provenance/health governance language that MCP should mirror without becoming an in-process Java extension.

### Existing Contract Documents
- `docs/phase-01-domain-contracts.md` — Runtime, event, session, workspace, and architecture boundary contract index.
- `docs/phase-02-cloud-server-api.md` — REST/SSE/session/run/event/audit API contract index and `pi-agent-client` boundary.
- `docs/phase-03-model-provider-contracts.md` — Provider/credential/resilience contract index and secret boundary.
- `docs/phase-04-governed-tool-contracts.md` — Governed tool descriptor, registry, gateway, policy, lifecycle, audit, redaction, and future MCP consumption guidance.
- `docs/phase-05-web-console.md` — Admin Governance placeholder/status patterns and public API/UI boundary.
- `docs/phase-06-extension-surface.md` — Source/capability/provenance/health/governance language and downstream note for MCP/plugin implementations.

### External Documentation to Research During Planning
- Spring AI MCP client documentation for Spring AI 1.1.x/available project version — supported transports, client configuration, tool discovery/call behavior, and Spring Boot starter integration.
- MCP Java SDK documentation — `McpSyncClient`, Streamable HTTP, stdio, legacy SSE transport status, tool list/call APIs, cancellation/timeout hooks, and server/tool change notifications.
- MCP specification transport/auth sections — Streamable HTTP, stdio, legacy SSE compatibility, authorization guidance, and tool annotations such as read-only/destructive/idempotent/open-world hints.
- Resilience4j documentation — reuse Phase 3-style timeout/retry/rate-limiter/circuit-breaker wrapping for remote MCP calls.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolDescriptor.java` — Canonical tool descriptor MCP tools must normalize into.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolProvenance.java` — Already includes `SourceKind.MCP` and `SourceKind.REMOTE`; use this instead of inventing a new source taxonomy.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolSchema.java` — Existing schema holder with dialect/document/sensitive fields/payload limits; MCP inputSchema should pass through here as JSON Schema.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRegistry.java` and `ToolExecutorBinding.java` — Registry/binding target for discovered MCP tools.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutionGateway.java` and `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` — Mandatory execution path that already handles validation, policy, preview, execution, redaction, audit, payload limits, timeout/cancellation, and lifecycle events.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/CredentialRef.java` and `SecretRef.java` — Existing credential/secret reference boundary for MCP auth/header injection.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPolicyEvaluator.java`, `ToolRedactor.java`, and `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/AuditRepository.java` — Existing policy/redaction/audit seams MCP must reuse.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEventType.java`, `RunEventPayload.java`, `EventSink.java`, and `RedactionMetadata.java` — Existing tool lifecycle and redaction event foundation.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java` and `DefaultExtensionContributionRegistry.java` — Structural patterns for adapting external capabilities to `ToolRegistry` while preserving source/provenance/governance metadata.
- `pi-agent-infrastructure-model-openai/src/main/java/io/github/pi_java/agent/infrastructure/model/openai/ProviderResiliencePolicy.java` and `OpenAiProviderProperties.java` resilience records — Patterns for remote-call resilience wrapping and typed properties.

### Established Patterns
- Domain/App/client contracts remain free of Spring, Vaadin, MCP SDK, Spring AI MCP, PF4J, DB, provider SDK, and implementation dependencies.
- External sources normalize into descriptor-first registries; runtime callers never use source-specific execution APIs.
- Admin Governance uses public DTOs and read-only status projections; Phase 7 may add refresh discovery but should not break the public API/UI boundary.
- Secret values are always represented by refs/redacted summaries outside infrastructure call boundaries.
- E2E tests use fake/no-key fixtures and assert events/audit/redaction through product paths.

### Integration Points
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ToolGovernanceBeanConfiguration.java` — Existing composite `ToolRegistry` wiring for built-in and extension tools; add MCP registry here or through imported MCP configuration.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` — Currently emits MCP placeholder status; replace with real MCP status/read model.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/controller/AdminGovernanceController.java` — Add or extend public Admin MCP status/refresh endpoint while preserving auth and DTO boundaries.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceOverviewResponse.java` and `GovernanceStatusDto.java` — Existing admin DTOs to extend for MCP server/tool status.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/admin` views/tests — Replace “MCP: UNCONFIGURED” placeholder with real server/tool/health/error status and refresh affordance.
- `pi-agent-adapter-web/src/test/java/io/github/pi_java/agent/adapter/web/ExtensionConformanceE2ETest.java` and `CloudServerGovernedToolE2ETest.java` — E2E templates for MCP discovery/execution through gateway, events, audit, and redaction.

</code_context>

<specifics>
## Specific Ideas

- User chose a configuration-file-first direction with DB/repository predesign only as a future seam; Admin remains read-only plus refresh discovery.
- User wants broad v1 transport coverage: Streamable HTTP, stdio, and legacy SSE compatibility, while keeping static CredentialRef-based auth and Tools-only protocol scope.
- User prefers an independent MCP registry/adapter, while reusing the source/capability/provenance/health language for governance consistency.
- User wants server-qualified tool IDs and JSON Schema passthrough.
- User selected “configured server is trusted” for connection posture, but confirmed retaining minimum safety checks for schemes, configured commands, redirects/header leakage, and sanitized errors.
- User wants MCP tool execution to remain explicitly allowlisted at the Agent/tool-scope level and conservative at policy level.
- User wants startup discovery plus manual Admin refresh, server/tool status retained when unhealthy, and standard tool failure/event/audit behavior rather than a large new MCP-specific event family.

</specifics>

<deferred>
## Deferred Ideas

- Admin UI/API CRUD for adding, editing, deleting, disabling, or dynamically hot-loading MCP servers — deferred beyond Phase 7.
- Full OAuth 2.1, Protected Resource Metadata, Dynamic Client Registration, token step-up flows, and broad OAuth client lifecycle management — deferred pending compatibility research.
- MCP resources, prompts, sampling, elicitation, and Pi acting as an MCP server — deferred; Phase 7 is MCP client Tools only.
- Complex periodic health polling, advanced retry dashboards, and full operational metrics/traces — defer deeper observability to Phase 9 unless needed for core Phase 7 validation.
- Dynamic plugin JAR loading and plugin lifecycle/quarantine — Phase 8.
- Production-grade sandboxing for arbitrary stdio/server processes — later hardening; Phase 7 stdio is trusted configured MCP server execution only.

</deferred>

---

*Phase: 07-mcp-client-bridge-and-governed-remote-tools*
*Context gathered: 2026-06-16*
