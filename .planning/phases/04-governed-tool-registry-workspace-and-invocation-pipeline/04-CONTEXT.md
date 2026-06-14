# Phase 4: Governed Tool Registry, Workspace, and Invocation Pipeline - Context

**Gathered:** 2026-06-14
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 4 builds the single governed safety gateway for every future tool source and workspace action before exposing Java SPI tools, Spring Bean tools, MCP tools, dynamic plugin tools, or Web Console approval cards. It must deliver canonical tool descriptors, a tool registry, a mandatory `ToolExecutionGateway`, schema validation, policy decisions, timeout/cancellation/payload limits, provision/preview, approval/sandbox gates, audit records, redacted lifecycle events, safe built-in example tools, and headless security E2E coverage.

This phase does **not** implement public SPI/Spring extension discovery, MCP client bridge, dynamic plugin loading, full production sandboxing for Coding Agents, unrestricted shell/file access, Web Console approval UI, or a general-purpose local developer CLI/TUI. Those capabilities must consume the Phase 4 gateway later rather than bypass it.

</domain>

<decisions>
## Implementation Decisions

### Gateway Contract and Registry Shape
- **D-01:** Tool registration must be **standardized descriptor-first**. All future sources — built-in tools, fake/testkit tools, SPI tools, Spring Bean tools, MCP tools, and plugin tools — must normalize into a `ToolDescriptor` plus executor binding before entering the registry. `ToolRegistry` must not expose source-specific APIs such as `registerSpringBean`, `registerMcpTool`, or `registerPluginTool` as its core contract.
- **D-02:** `ToolDescriptor` must use an explicit metadata model covering at least name/id, description, input schema, output schema or output type, provenance/source, version, scopes, risk level, side-effect classification, and timeout defaults. Planner/researcher may refine enum names and exact Java records, but the descriptor must carry enough metadata for policy, audit, Phase 5 tool cards, and Phase 7 MCP normalization.
- **D-03:** `ToolExecutionGateway` is the only allowed invocation path for tool execution. Its external responsibility is full-chain governance: resolve descriptor, validate arguments, evaluate policy, generate provision/preview where required, enforce approval/sandbox gates, apply timeout/cancellation/payload limits, execute the bound tool, normalize result/error, redact/summarize outputs, write audit, and emit lifecycle events.
- **D-04:** Existing `ToolInvoker`/`FakeToolInvoker` should become or be adapted as executor bindings behind the gateway. The General Agent loop and all testkit/runtime paths must be changed so they call `ToolExecutionGateway`, not `ToolInvoker` directly. A direct `ToolInvoker` invocation path may remain only as a low-level implementation detail that cannot be reached by runtime/provider/extension callers.
- **D-05:** The tool registry should mirror the Phase 3 provider-registry layering pattern: Domain owns framework-free descriptors/value objects, App owns registry/query/use-case ports, Infrastructure owns in-memory/JDBC/config implementations where needed, and Adapter Web wires beans and public API DTO mapping.

### Policy, Approval, Sandbox, and Preview Flow
- **D-06:** `REQUIRE_APPROVAL` must be modeled as a **suspend/wait** outcome, not as immediate deny. In Phase 4, the gateway should emit approval-required events/audit and return a state that can place the run/tool step into a waiting/suspended status. Phase 5 will provide the UI approval card and approval action, but Phase 4 must already preserve the future resumable approval contract.
- **D-07:** `REQUIRE_SANDBOX` is a **capability gate** in Phase 4. Do not pretend local-temp workspace execution is a production sandbox. If no compatible sandbox executor is available, return a require-sandbox blocked/suspended outcome with audit and events. Use fake sandbox support only to prove branch behavior in tests.
- **D-08:** Provision/preview is mandatory for risky actions. Side-effectful/destructive tools, workspace commands, or tools whose policy requires preview must generate a `ProvisionPreview`/impact estimate before execution. Read-only safe tools may skip preview unless policy requests it.
- **D-09:** Default policy engine should be conservative and configurable: allow safe read-only tools by default; require preview and/or approval for side-effectful tools; deny/block dangerous or destructive actions by default; honor `AgentDefinition.allowedToolScopes`, `policyRefs`, workspace policy, and future tenant/user policy context.
- **D-10:** Policy decisions must preserve all existing decision meanings: allow, deny, require approval, require sandbox, and block. Deny/block/approval/sandbox decisions must be observable through events and audit, and denied/blocked tool executions must not call the executor binding.

### Workspace Execution Model
- **D-11:** Phase 4 should include both deterministic fake workspace/command implementations and a bounded local-temp workspace/command implementation for dev/test E2E. The local-temp implementation must be workspace-root constrained, command-allowlist based, timeout/cancellation aware, and must not expose unrestricted host shell or filesystem as the default execution model.
- **D-12:** Workspace-backed capabilities should be exposed as a narrow governed tool set, not as a general shell/file toolkit. Prefer explicit tools such as workspace resource read, workspace artifact/resource write/append, and allowlisted command preview/execute. Each must have its own descriptor, policy behavior, lifecycle events, and audit records.
- **D-13:** Command allowlists and execution limits belong in Policy plus Workspace configuration. `CommandExecutionGateway` receives logical command requests, but whether a command is allowed must be decided from workspace policy, agent policy/tool scopes, descriptor risk/side-effect metadata, and runtime context — not from untrusted per-call declarations.
- **D-14:** Local-temp workspace data lifecycle is test/dev oriented and short-lived. Create workspace storage per run/session as needed and clean it up where safe. Persistent production workspace storage, Git checkout/patch workflows, and full Coding Agent workspace execution remain deferred.

### Redaction, Audit, Events, and Payload Limits
- **D-15:** Tool redaction should be schema-driven plus conservative by default. Tool descriptors/schemas may mark sensitive fields; `SecretRef`/`CredentialRef` values must only expose references/redacted metadata; unmarked large or unknown payloads should be summarized/truncated rather than blindly persisted.
- **D-16:** Tool lifecycle events should be a complete state machine suitable for Phase 5 tool cards: proposed, policy decided, preview generated, approval required, started, updated/progress, completed, failed, denied, and cancelled. Exact enum names are planner discretion, but TOOL-06 coverage must be explicit and public-event-friendly.
- **D-17:** Audit records should be decision-level and summary-level. Record policy decisions, preview generation, approval-required/approved/rejected when represented, execution start/outcome, deny/block/cancel/timeout, and security-sensitive workspace actions. Each audit entry should include tool ref, descriptor/provenance/version, risk/side-effect classification, principal/tenant/user, session/run/step/workspace/trace IDs, redacted input/output summary, decision reason, and error category where applicable.
- **D-18:** Payload limits and result summarization are mandatory gateway behavior, not optional later hardening. Apply separate limits for tool arguments, raw result bodies, RunEvent payloads, audit details, and public API DTOs. Oversized data should become summaries/truncation metadata or Artifact/Resource references, not unbounded event/audit blobs.
- **D-19:** Raw secrets and sensitive payloads must not appear in default RunEvents, REST/SSE responses, audit records, logs, prompts, future Admin views, or exception messages. E2E-06 should assert absence across API/runtime/events/audit/persistence paths using fake/configured sensitive values rather than real secrets.

### Built-in Example Tools and Registry API
- **D-20:** Phase 4 built-in examples should be a three-category minimum set: one safe read-only information tool, one workspace write artifact/resource tool, and one allowlisted command preview/execute tool. This set must cover success, deny, approval-required, provision preview, workspace-bound execution, redaction, and audit/event E2E without broad shell/file access.
- **D-21:** Side-effectful example tools should default to preview-then-approval behavior. They must prove that approval-required branches do not execute the side effect until approved/resumed, and that preview output is redacted/summarized appropriately.
- **D-22:** Example tools must not read arbitrary real environment variables, host system files, or sensitive host information. Use injected fake/config test secrets and workspace-scoped resources for redaction/security E2E.
- **D-23:** Phase 4 should directly provide a read-only Tool Registry REST/API surface, not only an internal App use case. The API should expose registry/catalog information needed by future Admin Governance and Web Console work while preserving client DTO boundaries and never leaking executor implementation details or raw secrets.

### Verification Requirements
- **D-24:** E2E must prove successful model-to-tool-to-model flow through `ToolExecutionGateway`, not a direct fake invoker path.
- **D-25:** E2E must prove policy deny and approval-required paths through runtime/API/events/audit/persistence, including prevention of unauthorized tool execution.
- **D-26:** E2E must prove provision preview, workspace-bound file/resource/command execution, payload summarization, and secret redaction paths.
- **D-27:** Architecture tests must continue enforcing COLA boundaries: Domain descriptors/policies/events remain framework-free; App ports/use cases do not depend on Spring/Jackson/provider SDK/MCP/PF4J; infrastructure/adapter modules own implementation dependencies.

### the agent's Discretion
- Exact Java record/interface names, package subdivisions, enum names, and internal service decomposition are left to planner/researcher discretion as long as every call path goes through one gateway and the descriptor-first registry remains source-agnostic.
- Exact JSON Schema validation library and schema draft/version are planner/researcher decisions, but validation must live outside Domain and be covered by contract tests.
- Exact REST endpoint paths and DTO names for the Tool Registry API are planner discretion, but the boundary must stay read-only in Phase 4 and client DTOs must not expose Domain/internal implementation details directly.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 4 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 4 — Phase goal, mapped requirements, success criteria, and research-needed note for JSON Schema validation/versioning and policy decision schema.
- `.planning/REQUIREMENTS.md` §Governed Tools — TOOL-01 through TOOL-07 details for descriptors, gateway, validation, policy, lifecycle events, and safe built-in tools.
- `.planning/REQUIREMENTS.md` §Workspace and Resources — WORK-03, WORK-07, and WORK-08 details for workspace-bound command execution, local-temp/fake implementations, and provision/preview.
- `.planning/REQUIREMENTS.md` §Observability, Policy, and Security — OPS-02, OPS-03, and OPS-05 details for audit, default policy engine, pluggable policy interface, and redaction.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E-02, E2E-03, and E2E-06 details for tool gateway success, deny/approval, and security redaction E2E.
- `.planning/PROJECT.md` — Product constraints: cloud safety, Workspace boundary, Java/COLA layering, extensibility, verification, and rejection of unrestricted shell/file execution.
- `.planning/STATE.md` — Current Phase 4 state and accumulated decisions from Phases 1-3.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Locked decisions for RunEvent envelope, tool/event/policy/workspace ports, fake General Agent loop, framework-free Domain, and deferred real workspace/tool execution.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Locked decisions for persist-then-emit events, JSONB payload schema discipline, App persistence/audit ports, client DTO boundaries, and composition-root ownership.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — Locked decisions for layered registry shape, provider-neutral descriptors, secret boundaries, resilience hooks, and no-key/fake E2E patterns to mirror for tools.
- `docs/phase-01-domain-contracts.md` — Contract index for runtime, WorkspaceGateway, CommandExecutionGateway, ToolInvoker, RunEvent, fake testkit, and Phase 4 deferrals.
- `docs/phase-02-cloud-server-api.md` — Cloud Server API/event/audit/persistence contract index confirming Phase 2 did not implement the governed gateway.
- `docs/phase-03-model-provider-contracts.md` — Provider registry/adapter contract index; use as a structural template for the tool registry and gateway documentation.

### Architecture and Research Guidance
- `.planning/research/ARCHITECTURE.md` — Tool registry and invocation pipeline design intent; especially the one-pipeline principle, descriptor normalization, executor binding, draft `ToolDescriptor`, and module ownership guidance.
- `.planning/research/PITFALLS.md` — Tool registry anti-patterns and single-gateway requirement; especially avoiding source-specific registration methods and bypass paths.
- `.planning/research/SUMMARY.md` — Research summary for `ToolDescriptor`, `ToolExecutorBinding`, `ToolExecutionGateway`, and governance-before-external-tools sequencing.
- `.planning/research/STACK.md` §MCP and Tool Integration — Project-owned Tool Registry requirement, JSON Schema contracts, and MCP-to-tool normalization guidance for later Phase 7.
- `.planning/research/STACK.md` §Resilience, Safety, and Governance — Resilience4j, default policy engine, and governance guidance.
- `.planning/research/STACK.md` §Testing and Quality — Testcontainers/fake external service expectations and architecture-boundary testing guidance.

### Code Templates and Integration Targets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderDescriptor.java` — Descriptor pattern to mirror for `ToolDescriptor`.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java` — Registry port pattern to mirror for `ToolRegistry`.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultModelProviderQueryService.java` — Query use-case pattern to mirror for Tool Registry query/API support.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/ModelProviderBeanConfiguration.java` — Adapter composition pattern to mirror for built-in tools, registry, policy, and gateway wiring.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/config/CloudRuntimeBeanConfiguration.java` — Main runtime composition root to extend with gateway-aware runtime/test wiring.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Current direct tool execution path to reroute through `ToolExecutionGateway`.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/FakeToolInvoker.java`, `FakePolicy.java`, `FakeWorkspaceGateway.java`, and `FakeCommandExecutionGateway.java` — Fake assets to evolve behind the gateway.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolInvoker.java` — Current low-level tool invocation port; should become an executor-binding implementation detail rather than a runtime-visible bypass path.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolCall.java` and `ToolResult.java` — Existing call/result records to evolve or wrap with gateway metadata, redaction summaries, and normalized error/result handling.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/policy/PolicyDecision.java` — Existing decision enum already supports ALLOW, DENY, REQUIRE_APPROVAL, REQUIRE_SANDBOX, and BLOCK.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceGateway.java` and `CommandExecutionGateway.java` — Workspace and command ports that Phase 4 must govern when exposed as tools.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/RunEvent.java`, `RunEventType.java`, `RunEventPayload.java`, `EventSink.java`, `RedactionMetadata.java`, and `EventVisibility.java` — Event envelope and redaction metadata foundation for tool lifecycle events.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/agent/AgentDefinition.java` and `RuntimeLimits.java` — Existing allowed tool scopes, policy refs, workspace policy ref, deadline, max steps, and max tool call hooks.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/persistence/AuditRepository.java` — Existing audit port to reuse/extend for tool policy decisions and execution outcomes.
- `pi-agent-infrastructure/src/main/java/io/github/pi_java/agent/infrastructure/event/PersistingEventSink.java` — Persist-then-emit event path that all new tool lifecycle events must use.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Integration target; current tool execution path calls policy/fake invoker directly and must be gateway-routed.

### Established Patterns
- Domain remains Java-only and framework-free, with ArchUnit enforcing no Spring/Jakarta/Jackson/PF4J/MCP/provider SDK/DB dependencies.
- App owns plain Java ports and use cases; Infrastructure/Adapter implement concrete persistence, registry config, web DTO mapping, and bean wiring.
- Provider registry from Phase 3 is the structural template: descriptor and capability records in Domain, registry ports/use cases in App, implementation/configuration in Infrastructure/Adapter, and public DTOs in client/adapter layers.
- Persist-then-emit is already established for `RunEvent`; Phase 4 must not emit direct live-only events for tool lifecycle.
- No-key deterministic fake E2E is the verification pattern. Phase 4 should extend fakes rather than requiring real external tools, shell access, or secrets.

### Integration Points
- Add `ToolDescriptor`, risk/side-effect/capability records, `ToolExecutorBinding`, policy/preview/result summary contracts, and lifecycle payloads in framework-free Domain packages.
- Add `ToolRegistry`, `ToolExecutionGateway`, policy/validation/redaction/audit orchestration ports/use cases in App where orchestration crosses persistence/audit/event boundaries.
- Add in-memory/fake registry and bounded local-temp workspace/command implementation in Infrastructure/Testkit as appropriate.
- Add read-only Tool Registry REST/API in Adapter Web/client DTOs for future Phase 5 Admin Governance reuse.
- Extend `RunEventType`/`RunEventPayload` for full tool lifecycle states and map them to public DTOs/SSE/event history.
- Extend Flyway/JDBC persistence/read models if new tool call/audit/projection state is required beyond existing `tool_calls`, `run_events`, and `audit_records` tables.

</code_context>

<specifics>
## Specific Ideas

- User chose to discuss all five gray areas and consistently selected the conservative recommended options for gateway, policy, workspace, redaction/audit/events, and example tools.
- Phase 4 should be explicit that local-temp workspace is a bounded dev/test executor, **not** a production sandbox.
- Tool Registry should have a read-only API in Phase 4 because Phase 5 Admin Governance needs tool catalog visibility.
- Side-effectful examples should demonstrate preview and approval-required paths before the Web Console exists; approval-required must not execute the side effect.

</specifics>

<deferred>
## Deferred Ideas

- Public Java SPI and Spring Bean/annotation tool registration — Phase 6. Phase 4 only establishes the normalized registry/gateway contracts they must use.
- MCP tool discovery/invocation and remote tool transport/auth controls — Phase 7. Phase 4 descriptors must be MCP-ready but should not implement MCP client bridge.
- Dynamic plugin tool registration/lifecycle/quarantine — Phase 8.
- Full production sandbox/Coding Agent workspace, Git checkout/patch/test workflows, and unrestricted shell/file/code execution — deferred beyond Phase 4 and explicitly not enabled by default.
- Web Console approval cards and tool execution cards — Phase 5 consumes the Phase 4 events/API/contracts.

</deferred>

---

*Phase: 04-governed-tool-registry-workspace-and-invocation-pipeline*
*Context gathered: 2026-06-14*
