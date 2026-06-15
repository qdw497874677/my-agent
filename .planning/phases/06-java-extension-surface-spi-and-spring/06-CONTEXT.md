# Phase 6: Java Extension Surface: SPI and Spring - Context

**Gathered:** 2026-06-15
**Status:** Ready for planning

<domain>
## Phase Boundary

Phase 6 stabilizes the Java-native in-process extension surface before MCP and dynamic plugin JAR loading. It must deliver public extension SDK/SPI contracts, Java `ServiceLoader` discovery, Spring Boot starter/autoconfiguration for Spring Bean and annotation registration, extension metadata/lifecycle/health/compatibility reporting, read-only Admin Governance visibility, and conformance tests proving extensions cannot bypass existing tool, policy, audit, event, credential, workspace, and redaction boundaries.

This phase does **not** implement MCP client/server transport, PF4J/dynamic classloader plugin loading, runtime hot reload/unload, Admin mutation controls for extension enable/disable, production sandboxing, a full Memory/RAG product, or new unrestricted tool/workspace capabilities.

</domain>

<decisions>
## Implementation Decisions

### Module Packaging
- **D-01:** Phase 6 should add a split extension module structure: a framework-free public extension SPI/SDK module plus a dedicated Spring Boot starter/autoconfiguration module. Do not collapse Spring support into the public SPI/SDK module.
- **D-02:** The public extension SPI/SDK module should depend only on Domain/App contracts needed to expose existing extension seams. It must not depend on Adapter Web, Infrastructure, Spring, Vaadin, PF4J, MCP, provider SDKs, JDBC, or other implementation modules.
- **D-03:** The Spring starter/autoconfiguration should be designed for external Spring Boot applications to reference, not only as internal Cloud Server wiring. The Cloud Server should consume this starter/autoconfiguration path itself so the external enterprise integration path is validated.
- **D-04:** Phase 6 should provide sample/test fixture extension artifacts rather than a production sample module. These fixtures should exercise `ServiceLoader`, Spring Bean, Spring annotation, and conformance paths without expanding the production surface.

### Extension Contract Shape
- **D-05:** The public extension SPI/SDK should cover the roadmap's full extension family in Phase 6: tools, model providers, policies, event sinks/listeners, workspace/resource providers, memory providers, and extension metadata/lifecycle. Some capabilities may be minimal contracts if the underlying runtime feature is intentionally deferred.
- **D-06:** Use an extension-source-with-capabilities model. Each extension exposes metadata plus one or more capability providers, such as tool providers, model provider contributors, policy providers, event sinks/listeners, workspace/resource providers, or memory providers. This keeps governance, health, compatibility, and source attribution coherent.
- **D-07:** Tool SPI contributors must submit a complete `ToolDescriptor` plus `ToolExecutorBinding`. The SPI path must remain descriptor-first and must not introduce tool-source-specific registry APIs or allow direct execution outside `ToolExecutionGateway`.
- **D-08:** Memory provider support should be a minimal SPI placeholder in Phase 6. Define the extension point and metadata shape, but do not wire Memory into the runtime loop, persistence model, vector stores, RAG behavior, or model context injection in this phase.

### Spring Registration Style
- **D-09:** Spring support should include both explicit Spring Bean registration and lightweight annotations. Beans are the primary path for complex capabilities such as providers, policies, workspace/resource providers, and advanced extension sources.
- **D-10:** Spring annotations in Phase 6 should be limited to lightweight Tools and Event listeners. Do not try to make model providers, policy engines, workspace providers, or memory providers fully annotation-driven in this phase.
- **D-11:** The Spring starter/autoconfiguration should discover SPI extensions and Spring extensions, then contribute deterministic composite registries/adapters into existing platform ports: tool registry/executor bindings, model provider registry, policy evaluator chain, event sink/listener bridge, workspace/resource provider registry, memory provider registry placeholder, and extension governance read model.
- **D-12:** Registration merge behavior must be deterministic and transparent. Use explicit order plus source metadata. Duplicate capability IDs should fail fast by default unless a deliberate configuration option says otherwise. Do not silently let later registrations override earlier ones.
- **D-13:** Spring wiring must follow existing project conventions: `@Configuration(proxyBeanMethods = false)`, `@ConditionalOnMissingBean`, explicit bean ownership, and no component-scan magic that makes extension boundaries hard to audit.

### Lifecycle and Governance
- **D-14:** Phase 6 lifecycle is startup/discovery oriented, not runtime hot loading. Model extension states such as `DISCOVERED`, `REGISTERED`, `FAILED`, and `DISABLED`. Do not implement dynamic load/unload/reload; that belongs to Phase 8 controlled plugin JARs.
- **D-15:** Enable/disable should be configuration-driven in Phase 6. Startup configuration may disable an extension source or individual capability so it is not registered/usable. Admin UI/API mutation controls for enable/disable are out of scope.
- **D-16:** Compatibility checks should use a platform API version range plus capability schema/contract version metadata. Incompatible extensions or capabilities must not register usable capabilities, and their compatibility failure must appear in governance/read-only status.
- **D-17:** Admin Governance should replace the Phase 5 extension placeholder with read-only real extension status. It should show extension sources, source kind (`SPI`, `SPRING_BEAN`, annotation, etc.), capability types and counts, registered/disabled/failed status, compatibility status, health summary, and redacted errors. It should not expose mutation controls.
- **D-18:** Lifecycle and governance records must preserve provenance. Tool, provider, policy, event, workspace, and memory capabilities should be traceable to an extension source and capability ID so later MCP/PF4J phases can reuse the same governance language.

### Conformance and Safety Boundaries
- **D-19:** Phase 6 conformance must cover the full safety boundary: extensions must not bypass `ToolExecutionGateway`, `ToolPolicyEvaluator`, audit records, `RunEvent`/`EventSink`, `CredentialRef`/`SecretRef`, Workspace boundaries, or Admin/public DTO redaction.
- **D-20:** The same conformance suite should validate both Java `ServiceLoader` sample extensions and Spring Bean/annotation sample extensions. SPI and Spring paths should prove identical normalized registration and governed execution semantics.
- **D-21:** Add architecture gates for the new extension SDK and Spring starter modules. The SDK must remain framework-free and must not depend on Spring/PF4J/MCP/Adapter/Infrastructure. Domain/App must not depend back on extension starter modules. Starter dependencies must stay outside Domain/App.
- **D-22:** Phase 6 must produce a contract document for downstream phases. It should list public extension APIs, `ServiceLoader` files, Spring starter usage, annotations, manifest/metadata fields, lifecycle/governance status semantics, compatibility rules, conformance requirements, and explicit deferrals to MCP, PF4J plugins, Memory/RAG, and production hardening.

### Folded Todos
- No pending todos matched Phase 6 scope.

### the agent's Discretion
- Exact module names are planner discretion, but they should clearly distinguish framework-free SDK/SPI from Spring Boot starter/autoconfiguration.
- Exact Java interface names, records, package subdivisions, and annotation names are planner discretion as long as the locked capability model and dependency boundaries hold.
- Exact compatibility version string/range format is planner discretion, but it must be machine-checkable and documented.
- Exact Admin Governance DTO names and endpoint paths are planner discretion, but the surface must stay read-only and use `pi-agent-client` DTO/public API boundaries.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Phase 6 Scope and Requirements
- `.planning/ROADMAP.md` §Phase 6 — Phase goal, EXT-01..EXT-05 and WORK-06 success criteria, and note that this establishes the public SDK shape before PF4J.
- `.planning/REQUIREMENTS.md` §Extension Fabric — EXT-01 through EXT-05 details for Java SPI, Spring Bean/annotation registration, public API/JAR metadata/lifecycle/health/conformance, Admin visibility, and boundary enforcement.
- `.planning/REQUIREMENTS.md` §Workspace and Resources — WORK-06 requirement for workspace/resource providers to be extendable without bypassing `ToolExecutionGateway`.
- `.planning/PROJECT.md` — Product constraints: Java-first, COLA layering, extension integration as v1 core value, cloud safety, Workspace boundary, verification, and reference boundary.
- `.planning/STATE.md` — Current Phase 6 state and accumulated decisions from completed phases.

### Prior Phase Contracts and Decisions
- `.planning/phases/01-runtime-spine-workspace-and-domain-contracts/01-CONTEXT.md` — Framework-free Domain, public contract layering, event envelope, Workspace ports, testkit, and strict architecture gates.
- `.planning/phases/02-cloud-server-persistence-sse-and-baseline-security/02-CONTEXT.md` — App/Infrastructure port ownership, `pi-agent-client` public DTO boundary, persist-then-emit event semantics, audit/persistence foundations, and composition-root ownership.
- `.planning/phases/03-model-provider-registry-and-openai-compatible-adapter/03-CONTEXT.md` — Layered provider registry, provider-neutral descriptors, `provider:model` model refs, `SecretRef`/`CredentialRef` boundaries, and no-key provider contract test patterns.
- `.planning/phases/04-governed-tool-registry-workspace-and-invocation-pipeline/04-CONTEXT.md` — Descriptor-first `ToolRegistry`, mandatory `ToolExecutionGateway`, policy/audit/redaction/preview semantics, lifecycle events, and future SPI/Spring deferral.
- `.planning/phases/05-agent-web-console-and-runtime-cockpit/05-CONTEXT.md` — Read-only Admin Governance placeholder decisions, public REST/SSE/read-model UI boundary, and extension/MCP/plugin deferrals.
- `docs/phase-01-domain-contracts.md` — Runtime, event, session, workspace, and testkit contract index.
- `docs/phase-02-cloud-server-api.md` — REST/SSE/session/run/event/audit API contract index and client DTO boundary.
- `docs/phase-03-model-provider-contracts.md` — Provider registry/adapter contract index and secret boundary.
- `docs/phase-04-governed-tool-contracts.md` — Governed tool descriptor, registry, gateway, policy, lifecycle, audit, redaction, and future SPI/Spring consumption guidance.
- `docs/phase-05-web-console.md` — Admin Governance extension placeholder, public API/UI boundary, and Phase 6 deferral note.

### Architecture and Stack Guidance
- `.planning/research/STACK.md` §Plugin and Extension System — Java SPI, Spring Bean discovery, and PF4J deferral guidance.
- `.planning/research/STACK.md` §MCP and Tool Integration — Project-owned Tool Registry and descriptor normalization guidance that extension tools must consume.
- `.planning/research/STACK.md` §Agent Runtime and AI Integration — Custom core plus adapter boundaries for provider extensions.
- `.planning/research/STACK.md` §Testing and Quality — ArchUnit, fake providers/tools, and conformance test expectations.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolDescriptor.java` — Canonical descriptor that SPI/Spring tool extensions must provide.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/tool/ToolProvenance.java` — Already reserves source kinds including `SPI`, `SPRING_BEAN`, `MCP`, `PLUGIN`, and `REMOTE`; Phase 6 should use this provenance rather than inventing a separate source model.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolRegistry.java` and `ToolExecutorBinding.java` — Descriptor plus executor-binding registration target for extension tools.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolExecutionGateway.java` and `pi-agent-app/src/main/java/io/github/pi_java/agent/app/usecase/DefaultToolExecutionGateway.java` — Mandatory governed execution path extensions must not bypass.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/model/ModelProviderRegistry.java` and `SecretResolver.java` — Provider and credential boundaries for model extension contributors.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/model/ProviderDescriptor.java` and `StreamingModelClient.java` — Provider-neutral model descriptors and streaming model port.
- `pi-agent-app/src/main/java/io/github/pi_java/agent/app/port/tool/ToolPolicyEvaluator.java` and `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/policy/PolicyDecision.java` — Policy extension target and existing decision semantics.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/event/EventSink.java`, plus infrastructure `PersistingEventSink` and `RunEventFanout` — Event publishing and fanout integration targets.
- `pi-agent-domain/src/main/java/io/github/pi_java/agent/domain/workspace/WorkspaceGateway.java` and `CommandExecutionGateway.java` — Workspace/resource extension targets and command boundary.
- `pi-testkit/src/main/java/io/github/pi_java/agent/testkit/GeneralAgentLoop.java` — Gateway-aware fake runtime useful for conformance testing.

### Established Patterns
- Domain production dependencies are empty and architecture tests forbid Spring, Jakarta, PF4J, MCP, Spring AI, Jackson, JDBC, provider SDKs, App, Infrastructure, and Adapter dependencies.
- App owns plain Java ports/use cases and depends on Domain/Client only. Extension SDK contracts should follow this style and avoid implementation dependencies.
- Infrastructure/Adapter modules own concrete implementations, Spring configuration, persistence, provider SDK integration, validation library integration, and public API mapping.
- Tool and provider systems already use descriptor-first registry patterns. Extension registration should compose into those registries rather than adding source-specific methods.
- Spring configuration currently uses explicit `@Configuration(proxyBeanMethods = false)` and `@ConditionalOnMissingBean` beans rather than broad hidden component scanning.
- Admin Governance already has extension/MCP/plugin placeholder slots from Phase 5; Phase 6 should make the extension slot real while MCP/plugin stay placeholders.

### Integration Points
- Add new Maven modules for extension SDK/SPI and Spring Boot starter/autoconfiguration in the root POM while preserving COLA dependency direction.
- Extend Adapter Web/Spring composition so Cloud Server consumes the Spring starter/autoconfiguration path rather than only manual internal wiring.
- Merge extension-discovered tools into the existing `ToolRegistry`/`ToolExecutionGateway` path.
- Merge extension-discovered provider capabilities into `ModelProviderRegistry` without exposing raw secrets or provider SDK types.
- Add extension governance read models behind existing Admin Governance API/UI patterns and `pi-agent-client` DTOs.
- Add conformance tests using ServiceLoader and Spring sample/test fixtures to prove all extension paths normalize through existing registries and governance boundaries.

</code_context>

<specifics>
## Specific Ideas

- User selected the recommended option for all discussed gray areas: split SDK/starter modules, full extension capability coverage, provider-of-capabilities model, Spring Beans plus lightweight annotations, startup lifecycle governance, config-driven disable, compatibility checks, read-only governance, full-boundary conformance, dual SPI/Spring sample validation, architecture gates, and a Phase 6 contract doc.
- Memory provider should exist as a minimal extension point only; do not expand Phase 6 into Memory/RAG runtime behavior.
- Spring annotations should stay lightweight in Phase 6: tools and event listeners only. Complex provider/policy/workspace/memory capabilities should use explicit Beans.
- Duplicate capability IDs should fail fast by default so extension conflicts are visible and safe.

</specifics>

<deferred>
## Deferred Ideas

- MCP server configuration, discovery, transport/auth, remote tool invocation, and SSRF controls — Phase 7.
- PF4J/dynamic plugin JAR loading, runtime classloader lifecycle, disable/quarantine operations, and plugin directory management — Phase 8.
- Runtime hot reload/unload of Java SPI/Spring extensions — out of Phase 6; revisit only if later product phases require it.
- Admin mutation controls for enabling/disabling extensions — deferred to plugin/governance phases; Phase 6 is config-driven and read-only in Admin.
- Full Memory/RAG/pgvector integration and model context injection — future Memory/RAG scope, not Phase 6.
- Production sandbox/Coding Agent workspace execution — later hardening beyond current local-temp/dev-test workspace support.

</deferred>

---

*Phase: 06-java-extension-surface-spi-and-spring*
*Context gathered: 2026-06-15*
