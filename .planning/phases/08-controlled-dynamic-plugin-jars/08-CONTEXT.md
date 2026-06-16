# Phase 8: Controlled Dynamic Plugin JARs - Context

**Gathered:** 2026-06-16
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 8 implements controlled, trusted enterprise plugin JAR support. It must provide a configurable controlled plugin directory, plugin descriptor loading, platform/API compatibility validation, lifecycle and health tracking, capability registration through the existing extension/gateway model, Admin Governance visibility, disable/quarantine operations, audit for plugin changes, and explicit documentation/runtime warnings that JVM plugin isolation is lifecycle/dependency isolation rather than a security sandbox.

This phase does **not** implement a plugin marketplace, Admin upload/install/delete/upgrade workflows, support for untrusted or semi-trusted arbitrary code, guaranteed JVM hot unload/classloader reclamation, production sandboxing, broad plugin permission systems, or full coverage of every possible extension capability type in the sample plugin.

</domain>

<decisions>
## Implementation Decisions

### Plugin Loading Model
- **D-01:** Use **PF4J plus a Pi extension bridge** as the plugin technology baseline. PF4J should own plugin JAR discovery, descriptor metadata, plugin classloaders, and lifecycle hooks; Pi should bridge plugin-provided capabilities back into the existing Phase 6 `ExtensionSource`/capability model.
- **D-02:** Plugin JARs should expose both plugin-level metadata and Pi extension capabilities. PF4J/plugin descriptor metadata identifies the plugin and lifecycle/package concerns, while one or more plugin-provided `ExtensionSource` instances expose Pi capabilities. Avoid replacing Phase 6 with a fully separate Plugin SPI.
- **D-03:** Plugin directory configuration is **configuration-file-first** through Spring/YAML/env typed properties: enable flag, controlled plugin directory, allowlist/selection controls, platform API version, and related safety settings. Admin should not upload, install, delete, or upgrade plugin JARs in Phase 8.
- **D-04:** Discovery/loading should run at startup and through an Admin-triggered manual refresh. Do not implement automatic hot directory watching in v1 because half-copied JARs, concurrent refresh, rollback, and classloader cleanup would expand scope and risk.
- **D-05:** PF4J and plugin classloader dependencies must live in one isolated infrastructure module, e.g. `pi-agent-infrastructure-plugin` or `pi-plugin-pf4j`. Domain, App, `pi-agent-extension-api`, `pi-agent-client`, `pi-agent-spring-boot-starter`, MCP infrastructure, and provider modules must not leak PF4J types.

### Lifecycle and Availability Semantics
- **D-06:** Reuse the Phase 6 `ExtensionLifecycleState` vocabulary for plugins: `DISCOVERED`, `LOADED`, `STARTED`, `DISABLED`, `FAILED`, and `QUARANTINED`. Plugin-specific records may add reason, sanitized error, descriptor metadata, JAR path summary, and operator action metadata.
- **D-07:** `disable` and `quarantine` affect new capability resolution and new tool calls. They should not forcibly interrupt already-running tool calls or runs in v1. Runtime consistency and auditability are preferred over abrupt cross-run termination.
- **D-08:** `DISABLED` means an administrator intentionally made the plugin unavailable. `QUARANTINED` is a stronger governance state used for compatibility failure, load/runtime safety concern, repeated failure, or explicit operator isolation. Quarantined plugins must be visibly marked, unusable for new resolutions, and require explicit operator action to leave quarantine.
- **D-09:** v1 should support disable/quarantine and optionally best-effort PF4J stop where safe, but must **not promise true JVM hot unload** or guaranteed classloader/resource reclamation. Full unload/reload guarantees remain out of scope.
- **D-10:** Failed or incompatible plugins must remain visible in Admin Governance with sanitized diagnostics. They must not silently disappear from the governance read model and must not contribute usable runtime capabilities.

### Admin Governance and Operations
- **D-11:** Phase 8 Admin scope is: view plugin status/details, trigger manual refresh/rediscovery, disable a plugin, and quarantine a plugin. Upload/install/delete/upgrade/version-management workflows are out of scope.
- **D-12:** Admin plugin views should show plugin ID, name, version, vendor, source kind, lifecycle state, enabled/available state, health, platform/API compatibility, capability list/counts, redacted descriptor metadata, load errors, compatibility errors, and quarantine/disable reasons.
- **D-13:** Admin plugin errors must be redacted/sanitized summaries. Do not expose raw exception bodies, raw file paths beyond safe configured-directory-relative summaries, environment variables, secrets, headers, or plugin-provided sensitive metadata in public DTOs/UI/log messages.
- **D-14:** Disable/quarantine operations require a confirmation step in the UI and may accept an optional operator reason. The reason should be included in audit and governance metadata after redaction/sanitization.
- **D-15:** Plugin mutation operations must be audited. Record actor/principal, tenant/user context when available, plugin ID, operation, optional reason, previous state, resulting state, timestamp, and sanitized failure details if the mutation fails.
- **D-16:** Admin plugin APIs and DTOs must stay behind the existing `pi-agent-client`/Adapter Web boundary. Do not expose Domain, PF4J, classloader, or extension implementation objects directly through public REST/Vaadin contracts.

### Safety, Policy, and Capability Registration
- **D-17:** Phase 8 trust posture is **trusted controlled directory, not a sandbox**. The product must clearly state that JVM classloader isolation is dependency/lifecycle isolation only and does not make untrusted plugin code safe.
- **D-18:** Plugin-provided tools and capabilities are visible only after descriptor/compatibility/lifecycle checks, but visible does not mean executable. Agents must still explicitly allow plugin tool scopes/tool IDs, and execution must still pass through the existing `ToolPolicyEvaluator` and `ToolExecutionGateway`.
- **D-19:** Plugin tools must normalize into `ToolDescriptor` plus `ToolExecutorBinding` and stamp `ToolProvenance.SourceKind.PLUGIN` plus plugin/source/capability IDs. No plugin-specific runtime invocation path or `registerPluginTool`-style API may bypass the source-agnostic registry/gateway pattern.
- **D-20:** Plugin capabilities should pass through the same deterministic contribution/compatibility/disablement/duplicate policy semantics as Phase 6 extensions. Duplicate capability IDs should fail fast by default unless an explicit configuration option allows overrides.
- **D-21:** Plugin-provided credentials, secrets, headers, arguments, results, health payloads, and errors must keep the existing `CredentialRef`/`SecretRef` and redaction boundaries. Raw secrets must not appear in Admin views, run events, audit, logs, exceptions, or public DTOs.

### Verification and Sample Plugin Scope
- **D-22:** The Phase 8 sample plugin JAR should focus on a safe read-only tool plus plugin metadata/health/compatibility. It must prove the plugin load → capability registration → `ToolGateway` invocation path without expanding the critical path to every capability family.
- **D-23:** Verification must include full product-path coverage: sample plugin JAR load, descriptor parsing, compatibility success and failure, capability registration, governed tool invocation through `ToolExecutionGateway`, disable behavior, quarantine behavior, Admin REST/UI status, audit/redaction assertions, and no-key deterministic execution.
- **D-24:** Architecture gates must enforce PF4J/plugin implementation isolation. Domain, App, extension API, client DTOs, Spring starter public API, MCP infrastructure, and model/provider infrastructure must not depend on `org.pf4j..` or plugin infrastructure implementation packages.
- **D-25:** Documentation produced in Phase 8 must include plugin packaging/descriptor expectations, controlled directory configuration, compatibility semantics, lifecycle/disable/quarantine behavior, Admin operations, audit/redaction guarantees, sample plugin usage, and the explicit non-sandbox warning.

### Folded Todos
- No pending todos matched Phase 8 scope.

### the agent's Discretion
- Exact module name, package layout, PF4J adapter class names, and whether plugin governance uses a dedicated `PluginGovernanceCatalog` port or extends the existing extension governance catalog are planner/researcher discretion, provided public boundaries and governance semantics above hold.
- Exact plugin property names and DTO field names are planner discretion, but they must be typed, redacted, documented, and compatible with existing Admin Governance patterns.
- Exact manual refresh implementation and best-effort PF4J stop behavior are planner discretion, provided v1 does not promise guaranteed hot unload.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 8 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 8 — Phase goal, PLUG-01 through PLUG-06 and E2E-08 success criteria, UI hint, and research-needed topics for PF4J, Spring Boot executable JAR packaging, classloader behavior, and unload semantics.
- `.planning/REQUIREMENTS.md` §Dynamic Plugins — PLUG-01 through PLUG-06 requirements for controlled plugin directory, descriptor loading, compatibility validation, lifecycle states, Admin visibility, disable/quarantine, and non-sandbox posture.
- `.planning/REQUIREMENTS.md` §End-to-End Verification — E2E-08 plugin requirement for sample plugin JAR loading/disable flows through the same `ToolExecutionGateway`, policy, audit, and event pipeline.
- `.planning/REQUIREMENTS.md` §Observability, Policy, and Security — OPS-02 plugin-change audit requirement and OPS-05 redaction constraints.
- `.planning/PROJECT.md` — Java-first, COLA boundaries, extension integration value, cloud safety, Workspace boundary, verification, and reference-boundary constraints.
- `.planning/STATE.md` — Current Phase 8 state and accumulated implementation decisions, including the open question around restart-required plugin unload.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Framework-free Domain, event/tool/policy/workspace ports, and strict no-PF4J architecture boundary.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — Public DTO boundary, audit/persistence ports, composition-root ownership, and persist-then-emit semantics.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — CredentialRef/SecretRef boundaries, provider isolation, resilience hooks, and no-key contract testing patterns.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Descriptor-first registry, mandatory `ToolExecutionGateway`, policy/preview/approval/sandbox semantics, audit, payload limits, redaction, and no source-specific tool APIs.
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Admin Governance public API/UI boundaries, plugin placeholder, inspect-only prior Admin posture, and Vaadin/public DTO patterns.
- `.planning/phases/06-java-extension-surface-spi-and-spring/06-CONTEXT.md` — Extension source/capability/provenance/health/compatibility language, config-driven disablement, deterministic contribution registry, and explicit Phase 8 deferrals.
- `.planning/phases/07-mcp-client-bridge-and-governed-remote-tools/07-CONTEXT.md` — Independent external-source registry pattern, governance consistency with Phase 6 language, read-only plus refresh Admin pattern, and gateway-only remote tool execution.

### Existing Contract Documents
- `docs/phase-01-domain-contracts.md` — Runtime, event, session, workspace, and architecture boundary contract index.
- `docs/phase-02-cloud-server-api.md` — REST/SSE/session/run/event/audit API contract index and `pi-agent-client` boundary.
- `docs/phase-03-model-provider-contracts.md` — Provider/credential/resilience contract index and secret boundary.
- `docs/phase-04-governed-tool-contracts.md` — Governed tool descriptor, registry, gateway, policy, lifecycle, audit, redaction, and future plugin consumption guidance.
- `docs/phase-05-web-console.md` — Admin Governance placeholder/status patterns and public API/UI boundary.
- `docs/phase-06-extension-surface.md` — Source/capability/provenance/health/governance language; line 145 explicitly tells Phase 8 plugin implementers to reuse the same source, capability, provenance, health, compatibility, enablement, and redacted error language.
- `docs/phase-07-mcp-client-bridge.md` — MCP adapter/governance pattern for an external source that stays isolated while reusing Pi tool governance. Read if present during planning.

### External Documentation to Research During Planning
- PF4J official documentation — plugin descriptor formats, plugin lifecycle, classloader behavior, plugin directory management, start/stop semantics, and integration patterns.
- Spring Boot executable/fat JAR packaging guidance — interaction between Boot classloaders, nested JARs, and external plugin directories.
- JVM classloader unload/resource cleanup references — limitations around threads, static state, open resources, and why v1 should not promise guaranteed hot unload.
- Maven plugin/test fixture guidance for building sample plugin JARs inside the reactor — deterministic no-key E2E packaging approach.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionSource.java` — Existing public extension entry point plugin JARs should bridge to rather than replace.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionMetadata.java` — Existing extension metadata shape with version, compatibility, lifecycle, health, enablement, and redacted metadata.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionLifecycleState.java` — Already includes `DISCOVERED`, `LOADED`, `STARTED`, `DISABLED`, `FAILED`, and `QUARANTINED`, matching Phase 8 lifecycle requirements.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ExtensionCompatibility.java` and `ExtensionApiVersion.java` — Existing machine-checkable platform API compatibility model.
- `pi-agent-extension-api/src/main/java/io/github/pi_java/agent/extension/api/ToolExtensionCapability.java` and related capability records — Existing typed capability model plugins should contribute through.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/DefaultExtensionContributionRegistry.java` — Existing deterministic contribution merge, compatibility, disabled source/capability, duplicate policy, and governance normalization pattern.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ServiceLoaderExtensionDiscovery.java` — Existing `ClassLoader`-aware ServiceLoader discovery and sanitized provider failure pattern that plugin classloaders can reuse or mirror.
- `pi-agent-infrastructure-extension/src/main/java/io/github/pi_java/agent/infrastructure/extension/ExtensionToolRegistry.java` — Existing adapter from `ToolExtensionCapability` into `ToolRegistry` with provenance metadata.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolProvenance.java` — Already includes `SourceKind.PLUGIN` for plugin-provided tools.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutionGateway.java` and `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` — Mandatory governed execution path for plugin tools.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultGovernanceQueryService.java` — Currently emits `futureStatus("plugins", "FUTURE_ENABLED", "Dynamic plugin governance arrives in Phase 8")`; Phase 8 should replace this placeholder with real plugin governance status.
- `pi-agent-adapter-web/src/main/java/io/github/pi_java/agent/adapter/web/ui/admin/AdminGovernanceOverviewView.java` and `AdminRegistryStatusView.java` — Existing Admin plugin status slot to replace with real plugin status/details/actions.
- `pi-agent-client/src/main/java/io/github/pi_java/agent/client/admin/GovernanceOverviewResponse.java` and `GovernanceStatusDto.java` — Existing public Admin DTO boundary for overview status; Phase 8 may add plugin-specific DTOs beside this pattern.

### Established Patterns
- Domain/App/client contracts are framework-free and must not depend on Spring, Vaadin, PF4J, MCP SDK, provider SDKs, JDBC, or implementation modules.
- External capability sources normalize into source-agnostic registries and governed execution. Runtime callers never call source-specific execution APIs.
- Admin Governance uses public `pi-agent-client` DTOs and Adapter Web/Vaadin mapping, not Domain or infrastructure objects directly.
- Extension and MCP integrations preserve failed/incompatible/unhealthy configured sources in governance with sanitized errors instead of hiding them.
- No-key deterministic tests and product-path E2E are the expected verification style.
- Architecture tests already anticipate PF4J isolation by forbidding `org.pf4j..` and plugin infrastructure package leaks in several existing modules.

### Integration Points
- Add a new isolated plugin infrastructure module to the root Maven reactor and dependency-management as needed.
- Add typed plugin configuration properties, likely under a `pi.plugins` prefix, in Adapter Web or a dedicated plugin Spring integration configuration.
- Bridge PF4J-discovered plugin classloaders to existing `ExtensionSource` discovery and `DefaultExtensionContributionRegistry` contribution flow.
- Compose plugin-provided `ToolRegistry`/capabilities into the primary governed registry after built-ins/extensions/MCP according to deterministic ordering and duplicate policy.
- Replace the plugin `FUTURE_ENABLED` placeholder in `DefaultGovernanceQueryService` with a real plugin governance read model.
- Extend Admin Governance REST/UI to show plugin details and expose refresh/disable/quarantine actions through public DTOs and audited App use cases.
- Add product-path tests using a sample plugin JAR and fake runtime/tool fixtures to verify governed invocation, disable/quarantine, Admin status, audit, and redaction.

</code_context>

<specifics>
## Specific Ideas

- User selected the recommended option for all discussed gray areas.
- Plugin loading should be PF4J-backed but Pi-extension-model-native: PF4J for JAR/classloader/lifecycle, Phase 6 extension source/capability model for platform capability registration.
- Admin disable/quarantine is the first real plugin mutation surface and should be intentionally narrow, confirmed, audited, and redacted.
- Disable/quarantine should stop new use without promising interruption of already-running calls or guaranteed hot unload.
- Sample plugin should be intentionally small: safe read-only tool plus metadata/health/compatibility, enough to prove E2E-08 without dragging every extension type into the critical path.

</specifics>

<deferred>
## Deferred Ideas

- Plugin marketplace, distribution, ratings, signing/review workflow, billing, and moderation — deferred beyond v1.
- Admin upload/install/delete/upgrade/version-management workflows for plugin JARs — deferred; Phase 8 uses configuration-file-first controlled directory management.
- Automatic directory hot watching and full hot reload/unload guarantees — deferred due JVM/resource/classloader complexity.
- Running untrusted or semi-trusted arbitrary plugin code — deferred until a real sandbox/permission/isolation model exists.
- Full sample coverage for model providers, policy providers, workspace providers, memory providers, and event listeners — deferred; Phase 8 sample focuses on governed tool plus metadata.

</deferred>

---

*Phase: 08-controlled-dynamic-plugin-jars*
*Context gathered: 2026-06-16*
