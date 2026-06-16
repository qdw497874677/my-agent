# Phase 06 Java Extension Surface Contracts

Phase 06 validates `WORK-06` and `EXT-01` through `EXT-05` for the Pi Java Agent Platform. It establishes the Java-native in-process extension surface for Java SPI and Spring Boot applications before Phase 7 MCP and Phase 8 dynamic plugin work build on the same governance language.

## Requirement Coverage

| Requirement | Phase 06 validation |
|-------------|---------------------|
| `WORK-06` | Workspace/resource providers are represented as extension capabilities and extension tools still enter runtime execution only as `ToolDescriptor` plus `ToolExecutorBinding`, so workspace actions cannot bypass `ToolExecutionGateway`. |
| `EXT-01` | `ExtensionSource` is the Java SPI entry point for tools, model providers, policies, event listeners, memory providers, workspace providers, and extension metadata. `ServiceLoaderExtensionDiscovery` loads these sources through Java `ServiceLoader`. |
| `EXT-02` | `pi-agent-spring-boot-starter` discovers explicit Spring `ExtensionSource` beans plus lightweight `@PiTool` and `@PiEventListener` method annotations without changing Domain/App/runtime core. |
| `EXT-03` | `pi-agent-extension-api` is the public extension API/JAR with API version compatibility, lifecycle, health, source metadata, typed capabilities, conformance fixtures, and ArchUnit gates. |
| `EXT-04` | `GET /api/admin/governance/extensions` and the Admin Registry Vaadin view expose extension source/capability health, compatibility, enablement, lifecycle, source kind, and redacted errors as read-only DTOs. |
| `EXT-05` | SPI/Spring conformance and Cloud Server E2E prove extension tools execute through `ToolExecutionGateway`, policy, audit, events, redaction, and credential boundaries. |

## Module Layout

| Module | Responsibility | Boundary |
|--------|----------------|----------|
| `pi-agent-extension-api` | Public framework-free SDK/SPI contracts: extension source metadata, API compatibility, lifecycle/health, and typed capability records. | Compile-depends only on `pi-agent-domain` and `pi-agent-app`. No Spring, PF4J, MCP, Vaadin, JDBC, Adapter, Infrastructure, Spring AI, or provider SDK dependencies. |
| `pi-agent-infrastructure-extension` | Java `ServiceLoader` discovery, deterministic contribution registry, compatibility/disablement/duplicate policy, and adapters into `ToolRegistry`, `ModelProviderRegistry`, and `ExtensionGovernanceCatalog`. | Infrastructure implementation module. No Spring starter, Adapter Web, PF4J, MCP, Vaadin, or provider SDK dependencies. |
| `pi-agent-spring-boot-starter` | Boot 3.5 auto-configuration for SPI discovery plus Spring Bean/annotation extension sources. | Spring integration module for external Boot applications and Cloud Server composition. Depends outward on extension API/App/infrastructure-extension only. |
| `pi-agent-adapter-web` | Product Cloud Server consumption of the starter plus public REST/Vaadin Admin Governance rendering. | Adapter layer only; exposes read-only public DTOs and no extension mutation controls. |

## Public SDK Contracts

The canonical extension entry point is:

- `io.github.pi_java.agent.extension.api.ExtensionSource`

Each source provides:

- `ExtensionMetadata` — extension id, name, version, vendor, supported platform API range, lifecycle state, health, enabled flag, and redacted metadata.
- `List<ExtensionCapability>` — one or more typed capability records.

Supported Phase 06 capability records are:

- `ToolExtensionCapability` — carries the canonical Phase 04 `ToolDescriptor` plus App `ToolExecutorBinding`.
- `ModelProviderExtensionCapability` — contributes provider/model metadata and `CredentialRef`-style credential references through `ExtensionModelProviderRegistry`.
- `PolicyExtensionCapability` — identifies policy extension metadata; runtime policy-chain expansion remains an explicit integration seam.
- `EventListenerExtensionCapability` — exposes event listener metadata and event type interests for governance/provenance.
- `WorkspaceProviderExtensionCapability` — identifies workspace/resource provider extension metadata without exposing host filesystem shortcuts.
- `MemoryProviderExtensionCapability` — metadata-only placeholder for memory provider extension identity; full Memory/RAG behavior is deferred.

Tool capabilities must be descriptor-first. A tool extension is not an execution API. It is a registration contribution that must later be invoked by runtime code through `ToolExecutionGateway`.

## ServiceLoader Usage

Java SPI extensions register provider files under:

```text
META-INF/services/io.github.pi_java.agent.extension.api.ExtensionSource
```

Each listed class must implement `ExtensionSource` and expose only redacted metadata. `ServiceLoaderExtensionDiscovery`:

1. loads `ExtensionSource` providers,
2. records failed providers as failed source statuses with sanitized errors,
3. merges optional explicit sources supplied by the Spring starter,
4. sorts deterministically by source order metadata and source id,
5. returns immutable discovery results.

`DefaultExtensionContributionRegistry` then applies platform API compatibility checks, disabled source/capability configuration, deterministic capability ordering, duplicate capability policy, and governance-visible status normalization.

## Spring Starter Usage

Spring applications add `pi-agent-spring-boot-starter`. The starter is registered through Boot 3.5 `AutoConfiguration.imports` and binds properties under `pi.extensions`:

- `pi.extensions.enabled` — disables the starter globally when false.
- `pi.extensions.platform-api-version` — platform API version used for compatibility checks.
- `pi.extensions.disabled-sources` — source ids excluded from usable registries but retained in governance status.
- `pi.extensions.disabled-capabilities` — capability ids excluded from usable registries but retained in governance status.
- `pi.extensions.allow-duplicate-capability-overrides` — opt-in duplicate override behavior; default is fail-fast.

The starter auto-configures, with `@ConditionalOnMissingBean` seams:

- `ServiceLoaderExtensionDiscovery`
- `DefaultExtensionContributionRegistry`
- `ExtensionToolRegistry`
- `ExtensionModelProviderRegistry`
- `ExtensionGovernanceCatalogAdapter`
- factories for explicit Spring beans and annotation-derived sources

External applications can contribute complex extensions by declaring Spring `ExtensionSource` beans. Cloud Server consumes this same starter path; it does not use a product-only extension registration shortcut.

## Annotations

Phase 06 annotations are intentionally limited to lightweight Spring method extensions:

- `@PiTool` — method-level tool metadata that the starter converts into a `ToolExtensionCapability` with a generated `ToolDescriptor` and executable `ToolExecutorBinding`.
- `@PiEventListener` — method-level event listener metadata that the starter exposes as governance-visible `EVENT_LISTENER` capabilities.

Annotation discovery inspects already-registered Spring beans through explicit starter factories. It does not component-scan arbitrary classes. Complex model providers, policy providers, workspace providers, and memory providers should use explicit `ExtensionSource` beans.

## Deterministic Merge and Duplicate Policy

All sources, whether Java SPI, explicit Spring Bean, or annotation-derived Spring source, pass through one `DefaultExtensionContributionRegistry`.

Merge rules:

1. order by source metadata `order`, then source id,
2. order capabilities by source order, source id, capability `order`, then capability id,
3. keep disabled/incompatible/failed contributions visible in governance,
4. expose only usable compatible/enabled capabilities in runtime registry adapters,
5. fail fast on duplicate capability ids by default,
6. allow duplicate overrides only when explicitly configured.

Built-in Adapter Web tool/model registries are composed ahead of extension registries, preserving built-in behavior without silently overriding platform-provided capabilities.

## Compatibility and Disablement

`ExtensionCompatibility` declares a machine-checkable platform API version range. Incompatible sources are not usable, but they remain visible in Admin Governance with `INCOMPATIBLE` status.

Disablement is configuration-driven in Phase 06:

- disabled sources and disabled capabilities are removed from usable registries,
- disabled contributions remain visible for operators,
- Admin REST/UI remains read-only and has no enable/disable mutation endpoint.

Runtime hot reload/unload is not part of Phase 06.

## Governance DTO/API/UI

Public governance flow:

```text
ExtensionSource / discovery
  -> DefaultExtensionContributionRegistry
  -> ExtensionGovernanceCatalogAdapter
  -> DefaultGovernanceQueryService
  -> pi-agent-client DTOs
  -> GET /api/admin/governance/extensions
  -> Admin Registry Vaadin view
```

Public DTOs are in `pi-agent-client` and contain only strings, booleans, lists, and `Map<String, String>` redacted metadata. The endpoint is authenticated and GET-only. It reports:

- source id, kind, name, version,
- lifecycle/usable status,
- health and compatibility,
- enabled state,
- redacted source error,
- capability id, type, status, enabled state, and redacted metadata.

Phase 7 MCP and Phase 8 PF4J/dynamic plugin implementers should reuse the same source, capability, provenance, health, compatibility, enablement, and redacted error language so Admin Governance stays consistent across extension mechanisms.

## Conformance Test Commands

All Phase 06 focused gates are no-key and do not require Docker.

Architecture gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api,pi-agent-spring-boot-starter -am -Dtest=ExtensionApiArchitectureTest,ExtensionStarterArchitectureTest test
```

SPI/Spring conformance gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-infrastructure-extension,pi-agent-spring-boot-starter -am -Dtest=ServiceLoaderConformanceTest,SpringExtensionConformanceTest test
```

Cloud Server product-path conformance and governance gates:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-adapter-web -am -Dtest=ExtensionGovernanceApiTest,ExtensionConformanceE2ETest test
```

Final focused Phase 06 no-key smoke gate:

```bash
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 mvn -q -pl pi-agent-extension-api,pi-agent-infrastructure-extension,pi-agent-spring-boot-starter,pi-agent-adapter-web -am -Dtest=ExtensionApiContractTest,ExtensionApiArchitectureTest,ServiceLoaderExtensionDiscoveryTest,ExtensionContributionRegistryTest,PiAgentExtensionAutoConfigurationTest,AnnotatedSpringExtensionTest,ServiceLoaderConformanceTest,SpringExtensionConformanceTest,ExtensionGovernanceApiTest,ExtensionConformanceE2ETest test
```

No-key fixtures use fake extension sources, fake tools, fake runtime wiring, fake policy outcomes, and redacted fake credential markers. Absence of provider API keys must never fail these tests.

## Safety Boundaries

Phase 06 extension loading preserves earlier safety contracts:

- Tool extensions contribute `ToolDescriptor` plus `ToolExecutorBinding`; runtime execution must go through `ToolExecutionGateway`.
- Tool policy, preview/approval gates, audit records, `tool.lifecycle` events, payload limiting, and redaction remain owned by the governed tool pipeline.
- Model provider extensions expose descriptors and `CredentialRef` boundaries; raw provider secrets are not public DTO or event data.
- Workspace/resource providers are extension metadata/capabilities only unless later wired through governed workspace/tool abstractions.
- Public Admin Governance is inspect-only; no mutation controls are added in this phase.
- Architecture tests prevent SDK/starter regressions into Spring AI, MCP, PF4J, Adapter, Infrastructure, Vaadin, JDBC, and provider SDK leakage where forbidden.

## Deferrals

- MCP transport/auth, trusted server configuration, discovery, remote invocation, and SSRF/network controls — Phase 7.
- PF4J dynamic classloading, plugin directory management, plugin lifecycle disable/quarantine, and sample plugin JAR loading — Phase 8.
- Runtime hot reload/unload of Java SPI or Spring extensions — deferred beyond Phase 06.
- Admin mutation controls for extension enable/disable — deferred; Phase 06 is configuration-driven and read-only.
- Full Memory/RAG indexing, retrieval, pgvector integration, and model context injection — deferred.
- Production sandboxing for coding/file/shell workspaces — deferred to production hardening beyond the current local-temp/dev-test workspace boundaries.
